package org.infinitytwo.umbralore.core.network.modern;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import org.infinitytwo.umbralore.core.data.PlayerData;
import org.infinitytwo.umbralore.core.entity.Player;
import org.infinitytwo.umbralore.core.exception.UnknownRegistryException;
import org.infinitytwo.umbralore.core.logging.Logger;
import org.infinitytwo.umbralore.core.manager.Players;
import org.infinitytwo.umbralore.core.registry.DimensionRegistry;
import org.infinitytwo.umbralore.core.security.AntiCheat;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.joml.Vector2i;

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

import static org.infinitytwo.umbralore.core.data.io.DataSchematica.Data;
import static org.infinitytwo.umbralore.core.network.modern.NetworkPackets.*;

public class ServerNetwork extends Network {
    private final Server server;
    private final Logger logger = new Logger(ServerNetwork.class);
    private final SecureRandom secureRandom = new SecureRandom();
    private final AntiCheat antiCheat = new AntiCheat(9);
    private final NetworkCommandProcessor processor;
    
    // --- Key Management ---
    private PrivateKey serverPrivateKey;
    private PublicKey serverPublicKey;
    private final Map<Connection, SecretKey> clientAesKeys = new ConcurrentHashMap<>();
    private volatile boolean started;
    
    public ServerNetwork(int udp, int tcp, NetworkCommandProcessor processor) {
        super(udp, tcp);
        this.processor = processor;
        
        // Initialize the KryoNet Server
        int readBufferSize = 131072;  // Keep this the same or slightly larger
        int writeBufferSize = 131072; // Increased from 16384 to 32768 (32 KB)
        
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
            logger.info("Server bound and listening on TCP/" + tcp + " and UDP/" + udp);
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
    
    // Note: Server typically sends to a specific connection, not to all (sendToAll... methods are for broadcast)
    @Override
    public void sendTCP(MPacket packet, Connection connection) {
        synchronized (server) { // <--- CRITICAL SYNCHRONIZATION POINT
            // Your logic to determine which connection to use, then:
            connection.sendTCP(packet); // KryoNet uses Kryo here
        }
    }
    
    @Override
    public void sendUDP(MPacket packet, Connection connection) {
        synchronized (server) { // <--- CRITICAL SYNCHRONIZATION POINT
            // Your logic to determine which connection to use, then:
            connection.sendUDP(packet); // KryoNet uses Kryo here
        }
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
        logger.info("Client connected: " + connection.getID());
        // Initiate handshake by requesting the client to send their AES key
        connection.sendTCP(new PUnencrypted("requestAesKey"));
    }
    
    @Override
    public void onReceive(Connection connection, Data packet) {
        // This is where you process fully decrypted and unserialized application data
        logger.debug("Received application data (" + packet.getClass().getSimpleName() + ") from " + connection.getID());
        
        processor.process(packet,connection);
    }
    
    @Override
    public void onDisconnect(Connection connection) {
        logger.info("Client disconnected: " + connection.getID());
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
                logger.info("Client " + connection.getID() + " requested public key. Sending...");
                
                PKey keyPacket = new PKey();
                keyPacket.rsaKey = serverPublicKey.getEncoded();
                connection.sendTCP(keyPacket);
            }
        }
        
        // 2. Client sends Encrypted AES Key
        else if (object instanceof PKey receivedKeyPacket) {
            try {
                logger.info("Handling encrypted AES key from client " + connection.getID() + " (RSA decrypt)...");
                
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, serverPrivateKey);
                byte[] aesKeyBytes = rsa.doFinal(receivedKeyPacket.rsaKey);
                
                SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
                clientAesKeys.put(connection, aesKey);
                
                // IMPORTANT: The PlayerData object needs to be fully instantiated (with UUID, name, etc.)
                // during a proper login handshake AFTER the key exchange.
                // This line is a temporary placeholder for testing the PlayerManager integration:
                Players.join(new PlayerData(connection.getRemoteAddressTCP().getAddress(),0,"TestUser" + connection.getID(), UUID.randomUUID(),"",false), connection);
                
                logger.info("AES key established for connection " + connection.getID() + ". Handshake complete.");
                
                connection.sendTCP(new PUnencrypted("connection TestUser" + connection.getID())); // Send confirmation
                
            } catch (GeneralSecurityException e) {
                logger.error("RSA Decryption of AES key failed. Closing connection " + connection.getID(), e);
                connection.close();
            }
        }
    }
    
    public void offlineMode(boolean offline) {
    
    }
    
    public boolean isStarted() {
        return started;
    }
}