package org.infinitytwo.umbralore.core.network;

import org.infinitytwo.umbralore.core.constants.LogicalSide;
import org.infinitytwo.umbralore.core.constants.PacketType;
import org.infinitytwo.umbralore.core.event.Event;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.network.NetworkFailure;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.infinitytwo.umbralore.core.constants.PacketType.*;

@Deprecated
public abstract class NetworkThread extends Thread {
    public final LogicalSide logicalSide;
    public static final int MAX_DATAGRAM_SIZE = 1024;
    public static final int PROTOCOL_MAX_HEADER_SIZE = (Integer.BYTES * 3) + (Short.BYTES * 2) + 1;
    protected static final int HEADER_SIZE = PROTOCOL_MAX_HEADER_SIZE;
    public static final int MAX_PAYLOAD_SIZE = MAX_DATAGRAM_SIZE - 1 - PROTOCOL_MAX_HEADER_SIZE;
    protected final int port;
    protected final EventBus eventBus;
    protected DatagramSocket socket;
    protected Map<Integer, List<Packet>> packetSent = new ConcurrentHashMap<>();
    protected volatile boolean close;
    private boolean started;
    
    public static List<byte[]> splitBytes(byte[] data, int maxChunkSize) {
        List<byte[]> chunks = new ArrayList<>();
        int length = data.length;

        for (int i = 0; i < length; i += maxChunkSize) {
            int end = Math.min(length, i + maxChunkSize);
            byte[] chunk = Arrays.copyOfRange(data, i, end);
            chunks.add(chunk);
        }

        return chunks;
    }

    public NetworkThread(LogicalSide side, EventBus eventBus, int port) {
        this.logicalSide = side;
        this.eventBus = eventBus;
        this.port = port;
    }

    @Override
    public void run() {
        System.out.println(eventBus.getProcess()+" is running.");
        try {
            this.socket = new DatagramSocket(port);

            // Use a buffer size that can safely handle the full MTU (1500)
            byte[] buffer = new byte[1500];
            started = false;

            while (!close) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                if (!started) eventBus.post(new InitializedEvent());
                started = true;
                socket.receive(packet);
                byte[] receivedData = Arrays.copyOf(packet.getData(), packet.getLength());
                Packet parsedPacket = read(receivedData, packet.getAddress(), packet.getPort());

                System.out.println("Received: "+parsedPacket);
                if (preProcessPacket(parsedPacket)) {
                    evaluate(parsedPacket);
                }
            }
            
        } catch (Exception e) {
            if (!close) {
                eventBus.post(new NetworkFailure(e));
                e.printStackTrace();
            }
            shutdown();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private boolean evaluate(Packet packet) {
        if (packet.type == CMD_BYTE_DATA.getByte()) {
            return true; // Pass to NetworkHandler for assembly
        }
        
        if (packet.type == NACK.getByte()) {
            ByteBuffer buffer = ByteBuffer.wrap(packet.payload);
            
            List<Packet> fragments = packetSent.get(packet.id);
            
            if (fragments == null) {
                System.out.println("Late NACK received for already completed packet ID: " + packet.id);
                return false;
            }
            
            short length = buffer.getShort();
            for (int i = 0; i < length; i++) {
                short index = buffer.getShort();
                
                // CRITICAL FIX: The index in fragments is 0-based. The NACK payload holds 0-based indices.
                if (index >= 0 && index < fragments.size()) {
                    resend(fragments.get(index), true); // Resend with encryption assumed
                } else {
                    System.err.println("NACK contained invalid fragment index: " + index + " for ID: " + packet.id);
                }
            }
            return false;
        }
        
        if (packet.type == ACK.getByte()) {
            // ACK received, stop tracking the reliable packet
            packetSent.remove(packet.id);
            return false;
            
        } else if (packet.type == COMMAND.getByte()) {
            byte[] payload = packet.payload;

            String command = new String(payload, StandardCharsets.UTF_8);
            if (command.split(" ")[0].equals("ping")) {
                pong(packet.address, packet.port);
            }
            // Internal commands shouldn't hit the event bus.
            return false;
        }

        return true;
    }

    public void pong(InetAddress address, int clientPort) {
        send("pong", address, clientPort, UNENCRYPTED.getByte(), false, false);
    }

    public void send(Packet packet, boolean isCritical) {
        send(packet,isCritical,true);
    }
    
    public void send(Packet packet, boolean isCritical, boolean encrypted) {
        send(packet.payload, packet.address, packet.port, packet.type, isCritical, encrypted);
    }

    public void shutdown() {
        close = true;
        if (socket != null) {
            socket.close();
        }
        interrupt();
    }

    public void send(@NotNull String msg, InetAddress clientAddress, int clientPort, boolean isCritical, boolean encrypted) {
        send(msg.getBytes(StandardCharsets.UTF_8), clientAddress, clientPort, COMMAND.getByte(), isCritical, encrypted);
    }

    public void send(byte[] bytes, InetAddress address, int clientPort, byte type, boolean isCritical, boolean encrypted) {
        send(ThreadLocalRandom.current().nextInt(), bytes, address, clientPort, type, isCritical, encrypted);
    }

    public void send(String msg, InetAddress address, int clientPort, byte type, boolean isCritical, boolean encrypted) {
        send(ThreadLocalRandom.current().nextInt(), msg.getBytes(StandardCharsets.UTF_8), address, clientPort, type, isCritical, encrypted);
    }
    
    public void resend(Packet packet, boolean encrypted) {
        // 1. Get the original full packet bytes (Header and Payload)
        byte[] fullPacket = packet.toBytes(); // Use the stored packet's serialization
        
        byte[] finalPayload;
        
        // 2. --- Encryption Logic ---
        if (encrypted) {
            byte[] encryptedData = encrypt(packet);
            
            finalPayload = ByteBuffer.allocate(1 + encryptedData.length)
                    .put((byte) 1) // encryption flag
                    .put(encryptedData)
                    .array();
        } else {
            finalPayload = ByteBuffer.allocate(1 + fullPacket.length)
                    .put((byte) 0) // no encryption
                    .put(fullPacket)
                    .array();
        }
        
        DatagramPacket datagramPacket = new DatagramPacket(finalPayload, finalPayload.length, packet.address, packet.port);
        
        try {
            socket.send(datagramPacket);
        } catch (IOException e) {
            eventBus.post(new NetworkFailure(e));
        }
    }
    
    public void send(int id, byte[] bytes, InetAddress clientAddress, int clientPort, byte type, boolean isCritical, boolean isEncrypted) {
        List<byte[]> splitPayloads = splitBytes(bytes, MAX_PAYLOAD_SIZE);
        if (splitPayloads.isEmpty()) return;
        
        List<Packet> packets = isCritical ? new ArrayList<>() : null;
        
        for (short i = 0; i < splitPayloads.size(); i++) {
            byte[] payload = splitPayloads.get(i);
            int nonce = ThreadLocalRandom.current().nextInt();
            
            // 1. Construct the internal Packet object FIRST
            Packet sendPacket = new Packet(id, nonce, payload.length, i, (short) splitPayloads.size(), type, payload, clientAddress, clientPort);
            if (isCritical) packets.add(sendPacket);
            
            // 2. Get the raw bytes from the Packet object
            byte[] rawPacketData = sendPacket.toBytes();
            
            byte[] finalPayload;
            
            // --- Encryption Logic ---
            System.out.println(isEncrypted);
            if (isEncrypted) {
                byte[] encrypted = encrypt(sendPacket);
                
                finalPayload = ByteBuffer.allocate(1 + encrypted.length)
                        .put((byte) 1) // encryption flag
                        .put(encrypted)
                        .array();
            } else {
                System.out.println("NO ENCRYPTION");
                // Use the bytes generated from toBytes()
                finalPayload = ByteBuffer.allocate(1 + rawPacketData.length)
                        .put((byte) 0) // no encryption
                        .put(rawPacketData)
                        .array();
            }
            
            DatagramPacket datagramPacket = new DatagramPacket(finalPayload, finalPayload.length, clientAddress, clientPort);
            
            try {
                socket.send(datagramPacket);
            } catch (IOException e) {
                eventBus.post(new NetworkFailure(e));
            }
        }
        
        if (isCritical) {
            packetSent.put(id, packets);
        }
    }
    public Packet read(byte[] packet, InetAddress address, int port) throws IOException {
        if (packet == null || packet.length < 1) {
            throw new IOException("Packet too short or null.");
        }

        byte encryptionFlag = packet[0];
        byte[] decrypted = Arrays.copyOfRange(packet, 1, packet.length);

        if (encryptionFlag == 1) {
            decrypted = decrypt(decrypted,address,port);

            // FIXED: Throw IOException on failure instead of RuntimeException
            if (decrypted.length < PROTOCOL_MAX_HEADER_SIZE) { // This now correctly uses 17
                throw new IOException("Decryption failed or resulting packet is too small.");
            }
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(decrypted);
        DataInputStream inStream = new DataInputStream(inputStream);

        int id = inStream.readInt();
        int nonce = inStream.readInt();
        int expectedPayloadLength = inStream.readInt();
        short index = inStream.readShort();
        short total = inStream.readShort();
        byte type = inStream.readByte();

        // Read the remaining payload
        byte[] payload = new byte[expectedPayloadLength];
        inStream.readFully(payload);

        return new Packet(id, nonce, payload.length, index, total, type, payload, address, port);
    }

    public int getPort() {
        return port;
    }

    public Map<Integer, List<Packet>> getPacketsSent() {
        return packetSent;
    }

    public void removePacketSent(int id) {
        packetSent.remove(id);
    }

    public boolean isClosed() {
        return close;
    }

    /**
     * Sends an ACK for a fully assembled packet (ID).
     * This method is usually called by the main thread after PacketAssembly confirms
     * all fragments have been received.
     * * NOTE: removePacketSent was removed from here because this ACK confirms a
     * received packet, not a sent one. The recipient handles tracking removal.
     * @param id The packet ID to acknowledge.
     * @param address The remote address.
     * @param clientPort The remote port.
     */
    public void sendACK(int id, InetAddress address, int clientPort) {
        sendACK(id,address,clientPort,false);
    }
    
    public void sendACK(int id, InetAddress address, int clientPort, boolean encrypted) {
        send(id, new byte[]{0,0,0,0}, address, clientPort, ACK.getByte(), false, encrypted);
    }
    
    public void sendACK(Packet packet) {
        sendACK(packet,false);
    }
    
    public void sendACK(Packet packet, boolean encrypted) {
        sendACK(packet.id,packet.address,packet.port,encrypted);
    }

    public void sendRequest(int id, PacketResendData data, InetAddress address, int clientPort) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES + data.index().length * Short.BYTES);

        buffer.putShort((short) data.index().length);
        for (short index : data.index()) {
            buffer.putShort(index);
        }

        byte[] payload = new byte[buffer.position()];
        buffer.flip();
        buffer.get(payload);

        send(id, payload, address, clientPort, NACK.getByte(), false, false);
    }

    public void ping(InetAddress address, int clientPort) {
        send("ping",address, clientPort, UNENCRYPTED.getByte(), false,false);
    }

    public void sendFailure(int id, String msg, InetAddress address, int clientPort) {
        sendFailure(id,msg,address,clientPort,true);
    }
    
    public void sendFailure(int id, String msg, InetAddress address, int clientPort, boolean encrypted) {
        send(id,msg.getBytes(StandardCharsets.UTF_8), address, clientPort, FAILURE.getByte(), false, encrypted);
    }
    
    public void sendFailure(Packet packet, String msg) {
        sendFailure(packet,msg,true);
    }
    
    public boolean isReady() {
        return started;
    }
    
    public void sendFailure(Packet packet, String msg, boolean encrypted) {
        sendFailure(packet.id,msg,packet.address,packet.port,encrypted);
    }
    
    @Deprecated
    public boolean isSelfPacket(Packet packet) {
        // 1. Check if the source address is the same as the local address
        boolean isLocalAddress = packet.address().equals(socket.getLocalAddress()) || packet.address().isLoopbackAddress();
        
        // 2. Check if the source port is the same as the local listening port
        boolean isLocalPort = packet.port() == socket.getLocalPort();
        
        return isLocalAddress && isLocalPort;
    }
    
    @Deprecated
    public record Packet(int id, int nonce, int length, short index, short total, byte type, byte[] payload, InetAddress address, int port) {
        public static Packet create(byte[] payload, byte type, InetAddress address, int port) {
            return new Packet(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt(), payload.length, (short) 0, (short) 1, type, payload, address, port);
        }
        
        public byte[] toBytes() {
            ByteBuffer b = ByteBuffer.allocate(HEADER_SIZE + payload.length);

            b.putInt(id);
            b.putInt(nonce);
            b.putInt(payload.length);
            b.putShort(index);
            b.putShort(total);
            b.put(type);
            b.put(payload);

            byte[] packet = new byte[b.position()];
            b.flip();
            b.get(packet);

            return packet;
        }

        @NotNull
        @Override
        public String toString() {
            return "Packet(id: "+id+", Nonce: "+nonce+", Packet: "+(index+1)+"/"+total+", Type: "+ PacketType.fromId(type) +", Address: "+address.toString()+", Port: "+port+")"; // SIR IT STARTS FROM 0
        }
    }
    
    @Deprecated
    public static class InitializedEvent extends Event {}

    protected abstract byte[] encrypt(Packet packet);
    protected abstract byte[] decrypt(byte[] data, java.net.InetAddress address, int port);
    protected abstract boolean preProcessPacket(Packet packet);
}