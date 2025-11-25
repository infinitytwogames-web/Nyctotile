package org.infinitytwo.nyctotile.core.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import org.infinitytwo.nyctotile.core.data.PlayerData;
import org.infinitytwo.nyctotile.core.manager.Players;
import org.infinitytwo.nyctotile.core.network.data.NetworkCommandProcessor;
import org.infinitytwo.nyctotile.core.security.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinitytwo.nyctotile.core.data.io.DataSchematica.Data;
import static org.infinitytwo.nyctotile.core.network.data.NetworkPackets.*;

public class ServerNetwork extends Network {
    private final Server server;
    private final Logger logger = LoggerFactory.getLogger(ServerNetwork.class);
    private final SecureRandom secureRandom = new SecureRandom();
    private final NetworkCommandProcessor processor;
    
    // --- Key Management ---
    private PrivateKey serverPrivateKey;
    private PublicKey serverPublicKey;
    private final Map<Connection, SecretKey> clientAesKeys = new ConcurrentHashMap<>();
    private volatile boolean started;
    private boolean online;
    
    public ServerNetwork(int udp, int tcp, NetworkCommandProcessor processor) {
        super(udp, tcp);
        this.processor = processor;
        
        // Initialize the KryoNet Server
        int readBufferSize = 131072;  // Keep this the same or slightly larger
        int writeBufferSize = 131072; // Increased from 16,384 to 32,768 (32 KB)
        
        this.server = new Server(readBufferSize, writeBufferSize);
        
        // 1. Generate keys first
        generateRSAKeyPair();
        
        // 2. Register packets
        register(server.getKryo());
        
        // 3. Attach the inherited listener
        server.addListener(getListener());
    }
    
    // --- Lifecycle and Operations Implementations ---
    
    @Override
    public void start() {
        try {
            server.bind(tcp, udp);
            server.start(); // Start the KryoNet server thread
            logger.info("Server bound and listening on TCP/{} and UDP/{}", tcp, udp);
            started = true;
        } catch (IOException e) {
            logger.error("Server failed to bind/start.", e);
        }
    }
    
    @Override
    public void shutdown() {
        logger.info("Shutting down server network.");
        server.stop();
    }
    
    @Override
    public void ping(Connection connection) {
        // Send a simple unencrypted control packet for ping
        connection.sendUDP(new PUnencrypted("ping"));
    }
    
    // --- Send Implementations ---
    
    // FIX 1: Removed unnecessary synchronization block to prevent performance bottleneck.
    @Override
    public void sendTCP(MPacket packet, Connection connection) {
        // KryoNet's connection.sendTCP is thread-safe.
        connection.sendTCP(packet);
    }
    
    // FIX 1: Removed unnecessary synchronization block to prevent performance bottleneck.
    @Override
    public void sendUDP(MPacket packet, Connection connection) {
        // KryoNet's connection.sendUDP is thread-safe.
        connection.sendUDP(packet);
    }
    
    // --- Security Implementations ---
    
    private void generateRSAKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            serverPrivateKey = pair.getPrivate();
            serverPublicKey = pair.getPublic();
            logger.info("RSA 2048 key pair generated successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }
    
    // AES Encryption (Used for application data)
    @Override
    protected byte[] encrypt(byte[] packet, Connection connection) throws Exception {
        SecretKey aesKey = clientAesKeys.get(connection);
        if (aesKey == null) {
            throw new IllegalStateException("Attempted to encrypt application data before AES key exchange complete.");
        }
        
        // Use AES/GCM/NoPadding for authenticated encryption (IV + Ciphertext + Tag)
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv); // Generate unique IV per packet
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); // 128-bit authentication tag
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] cipherText = cipher.doFinal(packet);
        
        // Prepend IV to the ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
        buffer.put(iv);
        buffer.put(cipherText);
        
        return buffer.array();
    }
    
    // AES Decryption (Used by the inherited listener)
    @Override
    protected byte[] decrypt(byte[] data, Connection connection) throws Exception {
        SecretKey aesKey = clientAesKeys.get(connection);
        if (aesKey == null) {
            // This case should not happen if the packet is PEncrypted, but is a fail-safe
            throw new IllegalStateException("No AES key for connection " + connection.getID() + " during decryption.");
        }
        
        // Data layout: IV (12 bytes) + Ciphertext + GCM Tag (16 bytes)
        final int MIN_ENCRYPTED_LENGTH = 12 + 16;
        
        if (data.length < MIN_ENCRYPTED_LENGTH) {
            throw new GeneralSecurityException("Encrypted data is too short. Expected minimum: " + MIN_ENCRYPTED_LENGTH + " bytes.");
        }
        
        byte[] iv = new byte[12];
        System.arraycopy(data, 0, iv, 0, 12);
        
        // The ciphertext includes the GCM Tag.
        // No need to subtract the tag size from the ciphertext length calculation.
        byte[] cipherText = new byte[data.length - 12];
        System.arraycopy(data, 12, cipherText, 0, data.length - 12);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
        
        // This line throws AEADBadTagException if the packet is corrupted/tampered with
        return cipher.doFinal(cipherText);
    }
    
    // --- Event Callback Implementations ---
    
    @Override
    public void onConnect(Connection connection) {
        logger.info("Client connected: {}", connection.getID());
        // Initiate handshake by requesting the client to send their AES key
        connection.sendTCP(new PUnencrypted("requestAesKey"));
    }
    
    @Override
    public void onReceive(Connection connection, Data packet) {
        // This is where you process fully decrypted and unserialized application data
        logger.debug("Received application data ({}) from {}", packet.getClass().getSimpleName(), connection.getID());
        
        processor.process(packet,connection);
    }
    
    @Override
    public void onDisconnect(Connection connection) {
        logger.info("Client disconnected: {}", connection.getID());
        clientAesKeys.remove(connection);
        
        // FIX 3: Clean up Player manager state
        Players.leave(connection);
    }
    
    @Override
    public void sendFailure(Connection connection, String s) {
        connection.sendTCP(new Failure(s));
    }
    
    @Override
    public void onControlPacket(Connection connection, Object object) {
        // This is where you handle handshake and control logic
        
        // 1. Client requests Public Key
        if (object instanceof PUnencrypted unencrypted) {
            if ("requestAesKey".equals(unencrypted.command)) {
                logger.info("Client {} requested public key. Sending...", connection.getID());
                
                PKey keyPacket = new PKey();
                keyPacket.rsaKey = serverPublicKey.getEncoded();
                connection.sendTCP(keyPacket);
            }
        }
        
        // 2. Client sends Encrypted AES Key
        else if (object instanceof PKey receivedKeyPacket) {
            try {
                logger.info("Handling encrypted AES key from client {} (RSA decrypt)...", connection.getID());
                
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, serverPrivateKey);
                byte[] aesKeyBytes = rsa.doFinal(receivedKeyPacket.rsaKey);
                
                SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
                clientAesKeys.put(connection, aesKey);
                
                // FIX 2: Removed the temporary PlayerData join here.
                // A player should only be "joined" after receiving PUserData (login) to avoid ghost players/UUID conflicts.
                
                logger.info("AES key established for connection {}. Handshake complete.", connection.getID());
                connection.sendTCP(new PHandshakeComplete()); // Send confirmation
                
            } catch (GeneralSecurityException e) {
                logger.error("RSA Decryption of AES key failed. Closing connection {}", connection.getID(), e);
                connection.close();
            }
        } else if (object instanceof PUserData data) {
            // Player login/authentication logic (Correctly handles joining the Player manager)
            if (online) {
                try {
                    Authentication.FirebaseTokenPayload payload = Authentication.verify(data.token);
                    String name = payload.name() + "#" + payload.uid().substring(0, 3);
                    
                    Players.join(new PlayerData(name, payload.uid(),data.token,true, connection));
                    connection.sendTCP(new PConnection(name, payload.uid()));
                    
                } catch (Exception e) {
                    logger.error("Failed to login",e);
                    sendFailure(connection,"AUTHENTICATION_ERROR token is invalid");
                }
            } else {
                UUID uid = UUID.randomUUID();
                
                Players.join(new PlayerData(data.name, uid.toString(), "",false,connection));
                connection.sendTCP(new PConnection(data.name, uid.toString()));
            }
        }
    }
    
    @Override
    public int getPortTCP() {
        return tcp;
    }
    
    @Override
    public int getPortUDP() {
        return udp;
    }
    
    public void offlineMode(boolean offline) {
        online = !offline;
    }
    
    public boolean isStarted() {
        return started;
    }
    
    public void broadcastTCP(Data data) {
        for (Connection connection : server.getConnections()) send(data,connection,true);
    }
    
    public void broadcastUDP(Data data) {
        for (Connection connection : server.getConnections()) send(data,connection,false);
    }
    
    public void broadcastUDPExcept(Connection connection, Data data) {
        server.sendToAllExceptUDP(connection.getID(),data);
    }
}