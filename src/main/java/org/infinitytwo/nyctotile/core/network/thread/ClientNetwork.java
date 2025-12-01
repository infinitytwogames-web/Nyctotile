package org.infinitytwo.nyctotile.core.network.thread;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import org.infinitytwo.nyctotile.core.network.Network;
import org.infinitytwo.nyctotile.core.network.data.NetworkCommandProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import static org.infinitytwo.nyctotile.core.data.io.DataSchematica.Data;
import static org.infinitytwo.nyctotile.core.network.data.NetworkPackets.*;

public class ClientNetwork extends Network {
    private final Client client;
    private final Logger logger = LoggerFactory.getLogger(ClientNetwork.class);
    private final SecureRandom secureRandom = new SecureRandom();
    private final String host;
    private final NetworkCommandProcessor processor;
    private volatile boolean isHandshakeComplete = false;
    
    // The client generates and stores its own Secret AES Key
    private SecretKey clientAesKey;
    private final String name;
    private final String token;
    
    public ClientNetwork(String host, int udp, int tcp, NetworkCommandProcessor processor, String name, String token) {
        super(udp, tcp);
        this.processor = processor;
        this.host = host;
        this.name = name;
        this.token = token;
        
        // Initialize the KryoNet Client
        this.client = new Client(131072,131072);
        
        // Register packets (must be the same order as the server)
        register(client.getKryo());
        
        // Attach the inherited listener
        client.addListener(getListener());
        
        // Start the client thread (mandatory before connecting)
        client.start();
        
        // Generate the symmetric key the client will use for the session
        generateClientAesKey();
    }
    
    public void awaitHandshakeCompletion() throws InterruptedException {
        // We use the 'this' object (ClientNetwork) for synchronization
        synchronized (this) {
            while (!isHandshakeComplete) {
                // Wait for the server to send PHandshakeComplete
                this.wait(100);
            }
        }
    }
    
    private void generateClientAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // 128-bit key size
            this.clientAesKey = keyGen.generateKey();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to generate client AES key.", e);
        }
    }
    
    // --- Lifecycle and Operations Implementations ---
    
    @Override
    public void start() {
        // Connection must be run in a separate thread as it is a blocking operation
        new Thread(() -> {
            try {
                logger.info("Connecting to {}:{}...", host, tcp);
                client.connect(5000, host, tcp, udp);
                logger.info("Client connected.");
            } catch (IOException e) {
                logger.error("Client failed to connect.", e);
                client.stop();
            }
        }).start();
    }
    
    @Override
    public void shutdown() {
        logger.info("Shutting down client network.");
        client.stop();
    }
    
    @Override
    public void ping(Connection connection) {
        // Send a simple unencrypted control packet for ping
        client.sendUDP(new PUnencrypted("ping"));
    }
    
    // --- Send Implementations ---
    
    // Client sends messages via its single Connection object
    @Override
    public void sendTCP(MPacket packet, Connection connection) {
        // Only send control packets (like the key exchange) before handshake is complete.
        if (!isHandshakeComplete && !(packet instanceof PKey) && !(packet instanceof PUnencrypted)) {
            logger.warn("Dropping TCP packet: Handshake not complete.");
            return;
        }
        client.sendTCP(packet);
    }
    
    @Override
    public void sendUDP(MPacket packet, Connection connection) {
        client.sendUDP(packet);
    }
    
    // --- Security Implementations ---
    
    // AES Encryption (Used for application data)
    @Override
    protected byte[] encrypt(byte[] packet, Connection connection) throws Exception {
        if (clientAesKey == null) {
            throw new IllegalStateException("Attempted to encrypt application data before AES key was set.");
        }
        
        // Use AES/GCM/NoPadding for authenticated encryption
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv); // Generate unique IV per packet
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv); // 128-bit authentication tag
        cipher.init(Cipher.ENCRYPT_MODE, clientAesKey, spec);
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
        if (clientAesKey == null) {
            throw new IllegalStateException("No AES key for decryption.");
        }
        
        // Data layout: IV (12 bytes) + Ciphertext + GCM Tag (16 bytes)
        if (data.length < 12 + 16) {
            throw new GeneralSecurityException("Encrypted data is too short.");
        }
        
        byte[] iv = new byte[12];
        System.arraycopy(data, 0, iv, 0, 12);
        
        byte[] cipherText = new byte[data.length - 12];
        System.arraycopy(data, 12, cipherText, 0, data.length - 12);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, clientAesKey, spec);
        
        // This throws AEADBadTagException if the packet is corrupted/tampered with
        return cipher.doFinal(cipherText);
    }
    
    // --- Event Callback Implementations ---
    
    @Override
    public void onConnect(Connection connection) {
        logger.info("Successfully established connection to server.");
        sendTCP(new PUnencrypted("requestAesKey"),null);
    }
    
    @Override
    public void onDisconnect(Connection connection) {
        logger.warn("Disconnected from server.");
    }
    
    @Override
    public void onReceive(Connection connection, Data packet) {
        processor.process(packet,connection);
    }
    
    @Override
    public void sendFailure(Connection connection, String s) {
        client.sendTCP(new Failure(s));
    }
    
    @Override
    public void onControlPacket(Connection connection, Object object) {
        // Handle the handshake process initiated by the server
        
        // 1. Server initiates the handshake
        if (object instanceof PUnencrypted unencrypted) {
            if ("requestAesKey".equals(unencrypted.command)) {
                // Server requested the key; send PUnencrypted("publicKey") response
                logger.debug("Server requested AES key. Waiting for Public Key...");
                
            } else if ("ping".equals(unencrypted.command)) {
                // Respond to server ping
                connection.sendUDP(new PUnencrypted("pong"));
            }
        }
        
        // 2. Server sends its Public Key
        else if (object instanceof PKey keyPacket && keyPacket.rsaKey != null) {
            try {
                logger.info("Received server Public Key. Encrypting and sending AES key...");
                
                // Decode the server's Public Key
                // --- Key Management ---
                // The client only needs the server's Public Key for encryption (during handshake)
                PublicKey serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyPacket.rsaKey));
                
                // Encrypt the client's generated AES key using the server's Public Key (RSA)
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.ENCRYPT_MODE, serverPublicKey);
                byte[] encryptedAesKey = rsa.doFinal(clientAesKey.getEncoded());
                
                // Send the encrypted AES key back to the server
                PKey responsePacket = new PKey();
                responsePacket.rsaKey = encryptedAesKey;
                connection.sendTCP(responsePacket);
                
            } catch (GeneralSecurityException e) {
                logger.error("RSA processing error during key exchange.", e);
                connection.close();
            }
        } else if (object instanceof PHandshakeComplete) {
            client.sendTCP(new PUserData(token,name));
            logger.info("Handshake complete! Sending player's data");
        } else if (object instanceof PConnection data) {
            logger.info("Connected to server! Ready to send encrypted data!");
            synchronized (this) { // Lock on the same object that awaitHandshakeCompletion() is waiting on.
                isHandshakeComplete = true;
                this.notifyAll(); // Wake up the waiting thread.
            }
        }
    }
    
    @Override
    public int getPortTCP() {
        return client.getRemoteAddressTCP().getPort();
    }
    
    @Override
    public int getPortUDP() {
        return client.getRemoteAddressUDP().getPort();
    }
    
    public void send(Data data, boolean critical) {
        if (!isHandshakeComplete) {
            logger.warn("Attempted to send encrypted application data before handshake completion. Dropping.");
            return;
        }
        send(data, null, critical);
    }
    
    public boolean isConnected() {
        return client.isConnected();
    }
}