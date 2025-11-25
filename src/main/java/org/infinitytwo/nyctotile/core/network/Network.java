package org.infinitytwo.nyctotile.core.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import org.infinitytwo.nyctotile.core.data.buffer.NByteBuffer;
import org.infinitytwo.nyctotile.core.data.io.DataSchematica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.infinitytwo.nyctotile.core.data.io.DataSchematica.Data;
import static org.infinitytwo.nyctotile.core.network.data.NetworkPackets.*;

public abstract class Network {
    protected final int udp, tcp;
    protected final Map<Integer, List<EncryptedPacket>> packets = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(Network.class);
    
    private final NetworkListener listener = new NetworkListener() {
        @Override
        public void connected(Connection connection) {
            onConnect(connection);
        }
        
        @Override
        public void received(Connection connection, Object object) {
            try {
                if (object instanceof EncryptedPacket e) {
                    
                    if (e.total > 1) {
                        // --- Fragmented Packet (TCP Reassembly) ---
                        
                        packets.computeIfAbsent(e.id, (integer -> new ArrayList<>()));
                        List<EncryptedPacket> packetList = packets.get(e.id);
                        packetList.add(e);
                        
                        if (packetList.size() == e.total) {
                            
                            // 1. CRITICAL FIX: Sort fragments by index before reassembly
                            packetList.sort(Comparator.comparing(p -> p.index));
                            
                            // 2. Calculate the total size of the final, encrypted data
                            int totalEncryptedSize = packetList.stream().mapToInt(p -> p.encrypted.length).sum();
                            
                            try (NByteBuffer buffer = new NByteBuffer()) {
                                // Ensure NByteBuffer has capacity to avoid unnecessary resizes during puts
                                buffer.ensureCapacity(totalEncryptedSize);
                                
                                // 3. Reassemble the ENCRYPTED payload (chunks)
                                for (EncryptedPacket packet : packetList) {
                                    buffer.put(packet.encrypted);
                                }
                                
                                buffer.flip();
                                
                                byte[] fullEncryptedData = new byte[buffer.remaining()];
                                buffer.get(fullEncryptedData);
                                
                                byte[] dataRaw = decrypt(fullEncryptedData, connection);
                                Data data = DataSchematica.deserialize(ByteBuffer.wrap(dataRaw));
                                
                                onReceive(connection, data);
                                
                                packets.remove(e.id);
                            }
                        }
                    } else {
                        // --- Unfragmented Packet (UDP or small TCP) ---
                        byte[] dataRaw = decrypt(e.encrypted, connection);
                        Data data = DataSchematica.deserialize(ByteBuffer.wrap(dataRaw));
                        onReceive(connection, data);
                    }
                    
                } else {
                    onControlPacket(connection, object);
                }
            } catch (Exception ex) {
                logger.error("Failed to process incoming packet from " + connection.getID(), ex);
                handleDecryptionFailure(connection, ex);
            }
        }
        
        @Override
        public void disconnected(Connection connection) {
            onDisconnect(connection);
        }
    };
    
    public Network(int udp, int tcp) {
        this.udp = udp;
        this.tcp = tcp;
    }
    
    public NetworkListener getListener() {
        return listener;
    }
    
    public void register(Kryo kryo) {
        kryo.register(MPacket.class);
        kryo.register(PKey.class);
        kryo.register(EncryptedPacket.class);
        kryo.register(PUnencrypted.class);
        kryo.register(Failure.class);
        kryo.register(PHandshakeComplete.class);
        kryo.register(PConnection.class);
        kryo.register(PUserData.class);
        
        kryo.register(byte[].class);
        kryo.register(String.class);
        kryo.register(short.class);
        kryo.register(short[].class);
    }
    
    // HERE IS THE CONFUSING PART!
    // THIS FRAGMENTS DATA SO BUFFER OVERFLOW SHOULDN'T HAPPEN
    public void send(Data packet, Connection connection, boolean critical) {
        byte[] payload = packet.serialize();
        
        // 2. CREATE THE DATASCHEMATICA HEADER
        String type = packet.getClass().getSimpleName().toLowerCase();
        byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
        
        // 3. Combine Header (length + name) + Payload
        ByteBuffer finalBuffer = ByteBuffer.allocate(Integer.BYTES + typeBytes.length + payload.length);
        finalBuffer.order(ByteOrder.LITTLE_ENDIAN); // Ensure consistent byte order
        
        // Write Header
        finalBuffer.putInt(typeBytes.length); // Write length of the class name
        finalBuffer.put(typeBytes);           // Write the class name bytes
        
        // Write Payload
        finalBuffer.put(payload);
        
        // The byte array ready for encryption now contains the required DataSchematica header
        byte[] finalPayload = finalBuffer.array();
        
        try {
            byte[] encrypted = encrypt(finalPayload, connection);
            List<byte[]> bytes = splitBytes(encrypted, 4096);
            int id = ThreadLocalRandom.current().nextInt();
            
            if (!critical) {
                sendUDP(new EncryptedPacket(encrypted,ThreadLocalRandom.current().nextInt(), (short) 0, (short) 1,id), connection);
                return;
            }
            
            for (short i =0; i < bytes.size(); i++) {
                sendTCP(new EncryptedPacket(bytes.get(i),ThreadLocalRandom.current().nextInt(),i, (short) bytes.size(),id), connection);
                
                // Start with 1ms.
                Thread.sleep(0,32);
            }
            
        } catch (Exception e) {
            logger.error("Failed to encrypt/send data", e);
        }
    }
    
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
    
    protected void handleDecryptionFailure(Connection connection, Exception ex) {
        logger.warn("Closing connection " + connection.getID() + " due to decryption failure.");
        connection.close();
    }
    
    // Lifecycle and operations
    public abstract void start();
    public abstract void shutdown();
    public abstract void ping(Connection connection);
    public abstract void sendTCP(MPacket packet, Connection connection);
    public abstract void sendUDP(MPacket packet, Connection connection);
    
    // Encryption/decryption
    protected abstract byte[] encrypt(byte[] packet, Connection connection) throws Exception;
    protected abstract byte[] decrypt(byte[] packet, Connection connection) throws Exception;
    
    // Event callbacks
    public abstract void onConnect(Connection connection);
    public abstract void onDisconnect(Connection connection);
    public abstract void onReceive(Connection connection, Data object);
    public abstract void sendFailure(Connection connection, String s);
    public abstract void onControlPacket(Connection connection, Object object);
    public abstract int getPortTCP();
    public abstract int getPortUDP();
}
