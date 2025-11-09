package org.infinitytwo.umbralore.core.network;

import org.infinitytwo.umbralore.core.constants.PacketType;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.network.PacketReceived;
import org.infinitytwo.umbralore.core.logging.Logger;
import org.infinitytwo.umbralore.core.network.NetworkThread.Packet;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.infinitytwo.umbralore.core.constants.PacketType.*;

@Deprecated
public class NetworkHandler extends Thread {
    private static final Logger logger = new Logger(NetworkHandler.class);
    private static final ConcurrentLinkedQueue<Packet> QUEUE = new ConcurrentLinkedQueue<>();
    
    public CommandProcessor getProcessor() {
        return processor;
    }
    
    @FunctionalInterface
    @Deprecated
    public interface CommandProcessor {
        void process(Packet packets, NetworkThread thread);
    }
    
    protected final EventBus eventBus;
    protected final PacketAssembly assembly;
    protected float delta;
    private long lastFrameTime;
    protected float blinkTimer;
    
    // --- TIMEOUT CONSTANTS ---
    protected final int TICK_RATE_MS = 10; // The sleep time (e.g., 10ms)
    protected final float CHECK_INTERVAL_SECONDS = 1.0f; // How often to check timers (1 second)
    // CRITICAL FIX: Ensure TICK_RATE_MS is not 0 to avoid division by zero if it were ever a variable.
    protected final int CHECK_INTERVAL_TICKS = (int) (CHECK_INTERVAL_SECONDS * 1000 / TICK_RATE_MS);
    
    // The timeout value in CHECK_INTERVAL_CYCLES (e.g., 3 cycles * 1 sec/cycle = 3 seconds).
    protected final int SENDER_TIMEOUT_CYCLES = 3;
    protected final int RECEIVER_INITIAL_TIMEOUT_CYCLES = 10; // Longer initial timeout for full assembly
    protected final int MAX_RESEND_ATTEMPTS = 5; // Maximum times a critical packet will be re-sent
    
    // Map of Packet ID to Time remaining until resend (Sender side) - Stored in TICK increments
    protected final Map<Integer, Integer> packets = new ConcurrentHashMap<>();
    // Map of Packet ID to Time remaining until resend request (Receiver side)
    protected final Map<Integer, Integer> receivedPackets = new ConcurrentHashMap<>();
    
    // Map of Packet ID to Current Resend Count (Used for both sender/receiver)
    protected final Map<Integer, Integer> resendAttempts = new ConcurrentHashMap<>();
    
    protected final NetworkThread networkThread;
    protected CommandProcessor processor;
    
    public NetworkHandler(EventBus bus, NetworkThread network, CommandProcessor processor) {
        eventBus = bus;
        this.processor = processor;
        eventBus.register(this);
        assembly = new PacketAssembly();
        networkThread = network;
        
        setDaemon(true);
    }
    
    public void setProcessor(CommandProcessor processor) {
        this.processor = processor;
    }
    
    // --- Message Queueing and Protocol Cleanup ---
    
    @SubscribeEvent
    public void onMessageReceived(PacketReceived e) {
        if (networkThread.isSelfPacket(e.packet)) {
            logger.warn("Discarding packet {} because it originated from this local socket.", e.packet.id());
            return;
        }
        
        if (e.packet.type() == ACK.getByte() || e.packet.type() == NACK.getByte() || e.packet.type() == FAILURE.getByte()) {
            
            logger.info("Received Protocol Packet: {} for ID {}", PacketType.fromId(e.packet.type()), e.packet.id());
            
            // ACK and FAILURE clear the reliable packet tracking
            if (e.packet.type() == ACK.getByte() || e.packet.type() == FAILURE.getByte()) {
                packets.remove(e.packet.id()); // Stop the sender timer
                resendAttempts.remove(e.packet.id()); // Remove the resend attempt counter
                networkThread.removePacketSent(e.packet.id()); // Stop tracking fragments
            }
            
            // NACK packets are handled by NetworkThread.evaluate()
            return;
        }
        
        // --- Data Packet Assembly Queueing ---
        if (e.packet.total() > 1) {
            logger.info("Received Fragment and attached to PacketAssembly: {}",e.packet.toString());
            QUEUE.add(e.packet);
            
            // Start a timer for the receiver to monitor assembly progress.
            // PutIfAbsent is correct to prevent resetting the timer on every fragment.
            receivedPackets.putIfAbsent(e.packet.id(), RECEIVER_INITIAL_TIMEOUT_CYCLES * CHECK_INTERVAL_TICKS);
            
        } else handle(e.packet); // Single-fragment packets are handled immediately
    }
    
    // --- Main Loop and Cleanup ---
    
    @Override
    public void run() {
        lastFrameTime = System.nanoTime();
        while (!isInterrupted()) {
            long now = System.nanoTime();
            delta = (now - lastFrameTime) / 1_000_000_000.0f; // Convert to seconds
            lastFrameTime = now;
            
            // 1. Process all queued fragments
            Packet packet;
            while ((packet = QUEUE.poll()) != null) {
                assembly.addPacket(packet);
            }
            
            // 2. Process completed assemblies and newly sent packets
            synchronize();
            
            // 3. Check for timeouts and handle reliability
            update();
            
            try {
                Thread.sleep(TICK_RATE_MS);
            } catch (InterruptedException e) {
                // Restore interrupt status and exit gracefully
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void cleanup() {
        assembly.discardAll();
        // Use interrupt to gracefully stop the thread
        interrupt();
    }
    
    // --- Reliability Timer Logic ---
    
    private void update() {
        blinkTimer += delta;
        
        // Check timers only when the interval hits the CHECK_INTERVAL_SECONDS (e.g., 1 second)
        if (blinkTimer >= CHECK_INTERVAL_SECONDS) {
            blinkTimer = 0; // Reset timer
            
            // --- 1. SENDER RELIABILITY (Outgoing Packets awaiting ACK) ---
            // Decrement by the number of ticks that passed (CHECK_INTERVAL_TICKS)
            packets.replaceAll((id, t) -> t - CHECK_INTERVAL_TICKS);
            
            Map<Integer, List<Packet>> sentPackets = networkThread.getPacketsSent();
            
            packets.entrySet().removeIf(entry -> {
                int id = entry.getKey();
                int remainingTicks = entry.getValue();
                
                // Check 1: Timer has expired (SENDER TIMEOUT)
                if (remainingTicks <= 0) {
                    
                    // Check 2: Has the packet been removed from sentPackets (ACKed) between timer checks?
                    if (!sentPackets.containsKey(id)) {
                        resendAttempts.remove(id);
                        return true; // Packet was ACKed, cleanup timer
                    }
                    
                    int attempts = resendAttempts.compute(id, (k, v) -> v == null ? 1 : v + 1);
                    
                    if (attempts > MAX_RESEND_ATTEMPTS) {
                        logger.error("CRITICAL FAILURE: Sent Packet ID " + id + " failed after " + MAX_RESEND_ATTEMPTS + " attempts without ACK/NACK. Dropping.");
                        networkThread.getPacketsSent().remove(id);
                        resendAttempts.remove(id);
                        return true;
                    }
                    
                    // Passive SENDER: We only warn and reset the timer to wait for the receiver's NACK
                    logger.warn("Sender Timeout: Waiting for receiver's NACK request for ID " + id + " (Attempt " + attempts + "/" + MAX_RESEND_ATTEMPTS + ")");
                    packets.put(id, SENDER_TIMEOUT_CYCLES * CHECK_INTERVAL_TICKS);
                    return false;
                }
                
                // If sentPackets was removed in onMessageReceived but timer hasn't expired yet
                if (!sentPackets.containsKey(id)) {
                    resendAttempts.remove(id);
                    return true;
                }
                return false;
            });
            
            
            // --- 2. RECEIVER RELIABILITY (Incoming Packets needing Assembly) ---
            receivedPackets.replaceAll((id, t) -> t - CHECK_INTERVAL_TICKS);
            
            receivedPackets.entrySet().removeIf((entry) -> {
                int id = entry.getKey();
                int remainingTicks = entry.getValue();
                
                // Check 1: Timer has expired (RECEIVER TIMEOUT)
                if (remainingTicks <= 0) {
                    
                    // If the assembly doesn't exist anymore, it was successfully processed by synchronize()
                    if (!assembly.exists(id)) return true;
                    
                    PacketResendData data = assembly.getMissingPackets(id);
                    
                    // If data is null, the assembly is COMPLETE but waiting for synchronize() to process it.
                    if (data == null) {
                        // Reset the timer with a short delay to give synchronize() a chance to run.
                        entry.setValue(CHECK_INTERVAL_TICKS);
                        return false;
                    }
                    
                    // Fragments are missing, proceed with NACK attempt logic
                    int attempts = resendAttempts.compute(id, (k, v) -> v == null ? 1 : v + 1);
                    
                    if (attempts > MAX_RESEND_ATTEMPTS) {
                        logger.error("CRITICAL FAILURE: Incoming packet ID " + id + " is incomplete after " + MAX_RESEND_ATTEMPTS + " request attempts. Discarding.");
                        assembly.discard(id);
                        resendAttempts.remove(id);
                        return true;
                    }
                    
                    // Request missing fragments (NACK)
                    NetworkThread.Packet firstPacket = assembly.getFirstPacket(id);
                    InetAddress address = assembly.getAddress(id);
                    
                    if (address != null && firstPacket != null) {
                        logger.warn("Receiver Timeout: Requesting missing fragments for ID " + id + " (Attempt " + attempts + "/" + MAX_RESEND_ATTEMPTS + ") - Missing: " + data.index().length + " fragments");
                        networkThread.sendRequest(id, data, address, firstPacket.port());
                        
                        // CRITICAL FIX: Use SENDER_TIMEOUT_CYCLES here too, as we wait for the sender to process the NACK.
                        receivedPackets.put(id, SENDER_TIMEOUT_CYCLES * CHECK_INTERVAL_TICKS);
                        return false;
                    }
                    
                    // Failed to get address/port, cleanup
                    resendAttempts.remove(id);
                    return true;
                }
                return false;
            });
        }
    }
    
    // --- Synchronization & Processing ---
    
    private void synchronize() {
        // --- 1. Receiver ACK/Confirmation & Processing ---
        List<Integer> packetsReady = assembly.getFilledPacketsId();
        
        if (!packetsReady.isEmpty()) {
            logger.error("DEBUG: Synchronize received {} ready IDs from Assembly: {}", packetsReady.size(), packetsReady);
        }
        
        logger.info("Synchronize check: {} packet assemblies ready.", packetsReady.size());
        
        for (int id : packetsReady) {
            
            // Retrieve essential metadata before getting the final data (which discards the assembly)
            InetAddress address = assembly.getAddress(id);
            NetworkThread.Packet firstPacket = assembly.getFirstPacket(id);
            
            try {
                if (address != null && firstPacket != null) {
                    
                    // 1. Process the fully assembled packet (calls assembly.discard(id) internally)
                    Packet fullyAssembledPacket = assembly.getData(id);
                    
                    // 2. Send confirmation (ACK) back to the sender
                    networkThread.sendACK(id, address, firstPacket.port(), true); // Assume critical packets use encryption
                    
                    logger.error("DEBUG: Processing final packet ID {}. Calling handle().", id);
                    
                    // 3. Cleanup timers and attempts
                    receivedPackets.remove(id);
                    resendAttempts.remove(id);
                    
                    // 4. Final delivery to the CommandProcessor
                    handle(fullyAssembledPacket);
                    
                } else {
                    // Should be highly rare, indicates a race condition between assembly completion and retrieval
                    logger.warn("Failed to find metadata for ready packet ID {} (possibly discarded by race). Discarding.", id);
                    assembly.discard(id);
                }
            } catch (MissingResourceException | NullPointerException e) {
                logger.error("CRITICAL FAILURE: Failed to retrieve or process ready packet ID {}: {}", id, e.getMessage());
                assembly.discard(id);
                receivedPackets.remove(id);
                resendAttempts.remove(id);
            }
        }
        
        // --- 2. Sender Timeout Tracking Initialization & Cleanup ---
        Map<Integer, List<NetworkThread.Packet>> sentPackets = networkThread.getPacketsSent();
        
        // CRITICAL LOGICAL CLEANUP: We only initialize timers here. Timer cleanup belongs in update() and onMessageReceived().
        for (int id : sentPackets.keySet()) {
            // Start the timer for any newly sent reliable packet
            packets.putIfAbsent(id, SENDER_TIMEOUT_CYCLES * CHECK_INTERVAL_TICKS);
        }
    }
    
    public void handle(Packet packets) {
        try {
            processor.process(packets,networkThread);
        } catch (Exception e) {
            logger.error("Error processing command for Packet ID {}: {}", packets.id(), e.getMessage());
        }
    }
    
    public NetworkThread.Packet getData(int id) throws MissingResourceException, NullPointerException {
        // Only call this when assembly.getFilledPacketsId() confirms readiness
        return assembly.getData(id);
    }
}