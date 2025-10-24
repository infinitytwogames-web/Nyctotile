package org.infinitytwo.umbralore.core.network;

import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.bus.LocalEventBus;
import org.infinitytwo.umbralore.core.event.network.PacketReceived;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NetworkHandler extends Thread {
    protected final LocalEventBus eventBus;
    protected final PacketAssembly assembly;
    protected float delta;
    private long lastFrameTime;
    protected float blinkTimer;
    protected final Map<Integer, Integer> packets = new ConcurrentHashMap<>();
    protected final Map<Integer, Integer> receivedPackets = new ConcurrentHashMap<>();
    protected final int time = 60;
    protected final NetworkThread networkThread;
    protected final NetworkCommandHandler commandHandler = new NetworkCommandHandler();
//    private final ServerThread server;

    public NetworkHandler(LocalEventBus bus, NetworkThread network) {
        eventBus = bus;
        eventBus.register(this);
        assembly = new PacketAssembly();
        networkThread = network;

        setName("Server Network Handler");
    }

    @SubscribeEvent
    public void onMessageReceived(PacketReceived e) {
        assembly.addPacket(e.packet.id(),e.packet);
        receivedPackets.putIfAbsent(e.packet.id(),time - 30);
    }

    @Deprecated
    public void registerCommand(NetworkCommand cmd) {

    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            long now = System.nanoTime();
            delta = (now - lastFrameTime) / 1_000_000_000.0f; // Convert to seconds
            lastFrameTime = now;

            synchronize();
            update();

            try {
                Thread.sleep(50); // Adjust to your tick rate
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void cleanup() {
        assembly.dropAll();
        interrupt();
    }

    private void update() {
        blinkTimer += delta;
        if (blinkTimer >= 1f) {
            blinkTimer = 0;
            packets.replaceAll((id, time) -> time - 1);
            packets.entrySet().removeIf(entry -> entry.getValue() <= 0);

            receivedPackets.replaceAll((id, time) -> time - 1);
            receivedPackets.entrySet().removeIf((entry) -> {
               if (entry.getValue() <= 0) {
                   int id = entry.getKey();
                   PacketResendData data = assembly.getMissingPackets(id);
                   if (data == null || !assembly.exists(id)) return true;
                   networkThread.sendRequest(id, data,assembly.getAddress(id), Objects.requireNonNull(assembly.getFirstPacket(id)).port());
                   return true;
               } else return false;
            });
        }
    }

    private void synchronize() {
        List<Integer> packetsReady = assembly.getFilledPacketsId();

        for (int id : packetsReady) {
            InetAddress address = assembly.getAddress(id);
            if (address != null) networkThread.sendConfirmation(id,address,assembly.getFirstPacket(id).port());
            else assembly.discard(id);
        }

        Map<Integer, List<NetworkThread.Packet>> packet = networkThread.getPacketsSent();

        for (int id : packet.keySet()) {
            packets.putIfAbsent(id, time);
        }

        packets.entrySet().removeIf(entry -> !packet.containsKey(entry.getKey()));
    }

    public byte[] handle(String cmd) {
        try {
//            return commandHandler.handleCommand(CommandContext.of(networkThread,cmd.split(" "),server));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public NetworkThread.Packet getData(int id) throws MissingResourceException, NullPointerException {
        return assembly.getData(id);
    }

    public abstract boolean send(String msg, InetAddress address, int port);
}
