package org.infinitytwo.umbralore.core.network;

import org.infinitytwo.umbralore.core.logging.Logger;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Deprecated
public final class PacketAssembly {
    private final Set<Integer> completedPacketIds = new ConcurrentSkipListSet<>();
    // Packet ID -> Fragment Index -> Packet
    private final Map<Integer, ConcurrentHashMap<Short, NetworkThread.Packet>> assemblies = new ConcurrentHashMap<>();
    private final Logger logger = new Logger(PacketAssembly.class);
    
    // --- Core Assembly Logic ---
    
    public void addPacket(NetworkThread.Packet data) {
        int packetId = data.id();
        
        // 1. Get or create the fragment map for this ID.
        // Use ConcurrentHashMap for thread-safe access to fragments.
        ConcurrentHashMap<Short, NetworkThread.Packet> fragmentMap = assemblies.computeIfAbsent(
                packetId,
                i -> new ConcurrentHashMap<>()
        );
        
        // 2. Insert the fragment. putIfAbsent is safer against duplicate processing.
        fragmentMap.put(data.index(), data);
        logger.info("Added {}", data.toString());
        
        // Optimization: Quick exit if not near completion
        if (fragmentMap.size() < data.total()) {
            return;
        }
        
        // 3. Final, definitive completion check using 0-based indexing.
        // We synchronize on completedPacketIds to ensure atomicity with getFilledPacketsId().
        if (fragmentMap.size() == data.total()) {
            if (areAllFragmentsPresent(fragmentMap, data.total())) {
                synchronized (completedPacketIds) {
                    // Check again and add if still complete and not already added
                    if (!completedPacketIds.contains(packetId)) {
                        completedPacketIds.add(packetId);
                        logger.error("DEBUG: ID {} **COMPLETED** and added to finished queue.", packetId);
                    }
                }
            }
        }
    }
    
    private boolean areAllFragmentsPresent(Map<Short, NetworkThread.Packet> fragmentMap, int totalFragments) {
        // CRITICAL FIX: The index runs from 0 up to totalFragments - 1 (0-based indexing).
        for (short i = 0; i < totalFragments; i++) {
            if (!fragmentMap.containsKey(i)) {
                return false;
            }
        }
        return true;
    }
    
    // --- Retrieval and Cleanup ---
    
    public List<Integer> getFilledPacketsId() {
        List<Integer> ids;
        
        // CRITICAL FIX: Atomically copy and clear to prevent lost IDs.
        synchronized (completedPacketIds) {
            ids = new ArrayList<>(completedPacketIds);
            completedPacketIds.clear();
        }
        
        if (!ids.isEmpty()) {
            logger.error("DEBUG: getFilledPacketsId retrieved {} IDs: {}", ids.size(), ids);
        }
        return ids;
    }
    
    public NetworkThread.Packet getData(int packetId) throws MissingResourceException, NullPointerException {
        // Must exist, as it came from completedPacketIds
        ConcurrentHashMap<Short, NetworkThread.Packet> fragmentMap = assemblies.get(packetId);
        
        if (fragmentMap == null || fragmentMap.isEmpty()) {
            throw new NullPointerException("The packet id does not exist or was concurrently discarded.");
        }
        
        NetworkThread.Packet firstPacket = fragmentMap.get((short) 0);
        int expectedChunks = firstPacket.total();
        
        // Safety check (should always pass if pulled from completedPacketIds)
        if (!areAllFragmentsPresent(fragmentMap, expectedChunks)) {
            // This is a grave threading error, but handled gracefully.
            throw new MissingResourceException("Packet ID " + packetId + " is incomplete, despite being marked ready.",
                    NetworkThread.Packet.class.getName(), String.valueOf(packetId));
        }
        
        // Reconstruct payload
        int totalSize = 0;
        for (NetworkThread.Packet fragment : fragmentMap.values()) {
            totalSize += fragment.payload().length;
        }
        
        byte[] finalPayload = new byte[totalSize];
        int offset = 0;
        
        // Iterate from index 0 up to total-1 to ensure correct order
        for (short i = 0; i < expectedChunks; i++) {
            byte[] payload = fragmentMap.get(i).payload();
            System.arraycopy(payload, 0, finalPayload, offset, payload.length);
            offset += payload.length;
        }
        
        // Create the final assembled packet using data from index 0
        NetworkThread.Packet newPacket = new NetworkThread.Packet(
                packetId, firstPacket.nonce(), finalPayload.length,
                (short) 0, (short) 1, // Final packet is 1 chunk total, index 0
                firstPacket.type(), finalPayload, firstPacket.address(), firstPacket.port()
        );
        
        discard(packetId);
        return newPacket;
    }
    
    public void discard(int packetId) {
        assemblies.remove(packetId);
    }
    
    // --- NACK / Missing Packet Logic ---
    
    public PacketResendData getMissingPackets(int id) {
        ConcurrentHashMap<Short, NetworkThread.Packet> receivedPackets = assemblies.get(id);
        
        if (receivedPackets == null || receivedPackets.isEmpty()) return null;
        
        // Get total from the original packet index 0, if available. Fallback to any.
        short total = receivedPackets.get((short) 0) != null ? receivedPackets.get((short) 0).total() : receivedPackets.values().iterator().next().total();
        
        List<Short> missingIndexes = new ArrayList<>();
        
        // Iterate through all expected indices (0 to total-1)
        for (short i = 0; i < total; i++) {
            if (!receivedPackets.containsKey(i)) {
                missingIndexes.add(i);
            }
        }
        
        if (missingIndexes.isEmpty()) return null;
        
        short[] missingArray = new short[missingIndexes.size()];
        for (int j = 0; j < missingArray.length; j++) {
            missingArray[j] = missingIndexes.get(j);
        }
        
        return new PacketResendData(id, missingArray);
    }
    
    // --- Accessors ---
    
    public InetAddress getAddress(int id) {
        ConcurrentHashMap<Short, NetworkThread.Packet> map = assemblies.get(id);
        if (map != null && map.get((short) 0) != null) {
            return map.get((short) 0).address();
        } else if (map != null && !map.isEmpty()) {
            // Fallback to any fragment if index 0 is missing
            return map.values().iterator().next().address();
        }
        return null;
    }
    
    public boolean exists(int id) {
        return assemblies.containsKey(id);
    }
    
    public NetworkThread.Packet getFirstPacket(int id) {
        ConcurrentHashMap<Short, NetworkThread.Packet> p = assemblies.get(id);
        if (p == null || p.isEmpty()) return null;
        
        return p.get((short) 0);
    }
    
    public void discardAll() {
    
    }
}