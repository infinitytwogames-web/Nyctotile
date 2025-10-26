package org.infinitytwo.umbralore.core.network.server;

import org.infinitytwo.umbralore.core.manager.Players;
import org.infinitytwo.umbralore.core.constants.LogicalSide;
import org.infinitytwo.umbralore.core.data.PlayerData;
import org.infinitytwo.umbralore.core.event.bus.LocalEventBus;
import org.infinitytwo.umbralore.core.network.NetworkThread;
import org.infinitytwo.umbralore.core.security.Authentication;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infinitytwo.umbralore.core.constants.PacketType.*;

public final class ServerNetworkThread extends NetworkThread {
    private PublicKey clientPublicKey;
    private PrivateKey serverPrivateKey;
    private PublicKey serverPublicKey;
    private SecretKey aesKey;

    private boolean onlineMode = true;

    public ServerNetworkThread(LocalEventBus eventBus, int port) {
        super(LogicalSide.SERVER, eventBus, port);
        setName("Server Network Thread");
        generateRSAKeyPair();
    }

    private void generateRSAKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            serverPrivateKey = pair.getPrivate();
            serverPublicKey = pair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    @Override
    protected byte[] encrypt(Packet packet) {
        if (aesKey == null) {
            System.out.println("Encryption skipped: AES key is null.");
            return packet.toBytes();
        }

        try {
            System.out.println("Encrypting packet ID: " + packet.id());
            byte[] plain = packet.toBytes();
            byte[] iv = new byte[12];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
            byte[] cipherText = cipher.doFinal(plain);

            ByteBuffer buffer = ByteBuffer.allocate(1 + iv.length + cipherText.length);
            buffer.put((byte) 1);
            buffer.put(iv);
            buffer.put(cipherText);

            return buffer.array();
        } catch (Exception e) {
            System.err.println("Encryption failed:");
            e.printStackTrace();
            return packet.toBytes();
        }
    }

    @Override
    protected byte[] decrypt(byte[] data) {
        if (aesKey == null || data[0] != 1) {
            System.out.println("Decryption skipped: AES key is null or data is unencrypted.");
            return data;
        }

        try {
            System.out.println("Decrypting packet...");
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.get(); // skip flag

            byte[] iv = new byte[12];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            System.err.println("Decryption failed:");
            e.printStackTrace();
            return data;
        }
    }

    @Override
    protected boolean preProcessPacket(Packet packet) {
        byte[] rawPayload = packet.payload();
        byte[] payload;
        UUID uuid = null;

        if (packet.type() == UNENCRYPTED.getType() ||
            packet.type() == EXCHANGE.getType()
        ) {
            payload = rawPayload;
        } else {
            byte[] p = decrypt(rawPayload);
            byte[] eUUID = new byte[16];
            payload = new byte[p.length - 16];
            
            System.arraycopy(p,0,eUUID,0,16);
            System.arraycopy(p,16,payload,0,p.length -16);
            
            uuid = bytesToUUID(eUUID);
        }

        System.out.println("Pre-processing packet. Type: " + packet.type() + ", ID: " + packet.id());

        if (packet.type() == UNENCRYPTED.getType()) {
            String[] cmd = new String(payload, UTF_8).split(" ");
            System.out.println("Received command: " + cmd[0]);

            if (cmd[0].equals("publicKey")) {
                System.out.println("Sending public RSA key to client.");
                send(serverPublicKey.getEncoded(), packet.address(), packet.port(), EXCHANGE.getType(), true, false);
                return false;
            } else if (cmd[0].equals("ping")) {
                System.out.println("Ping received. Responding with pong.");
                pong(packet.address(),packet.port());
                return false;
            }
        }

        if (packet.type() == AUTHENTICATION.getType()) {
            System.out.println("Authentication packet received.");
            authenticate(packet);
            return false;
        }

        if (packet.type() == COMMAND.getType()) {
            // TODO: MORE COMMANDS
        }

        if (packet.type() == EXCHANGE.getType()) {
            try {
                System.out.println("Handling RSA/AES key exchange from client...");
                X509EncodedKeySpec spec = new X509EncodedKeySpec(payload);
                clientPublicKey = KeyFactory.getInstance("RSA").generatePublic(spec);

                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                aesKey = keyGen.generateKey();

                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.ENCRYPT_MODE, clientPublicKey);
                byte[] encryptedAesKey = rsa.doFinal(aesKey.getEncoded());

                System.out.println("Sending encrypted AES key to client.");
                send(encryptedAesKey, packet.address(), packet.port(), EXCHANGE.getType(), true, false);
            } catch (GeneralSecurityException e) {
                System.err.println("Key exchange failed:");
                e.printStackTrace();
            }
            return false;
        }
        
        PlayerData playerData = Players.getPlayerById(uuid);

        if (playerData == null) {
            System.out.println("Unknown player UUID: " + uuid);
            return false;
        }

        return true;
    }

    private void authenticate(Packet packet) {
        // This method is called after the packet has been decrypted (if an AES key exists).
        // The payload should be structured as: [16-byte UUID] + [4-byte token length] + [token bytes]

        // We must send a confirmation to acknowledge receipt of the authentication packet.
        sendConfirmation(packet.id(), packet.address(), packet.port());

        System.out.println("Authenticating client at: " + packet.address());

        UUID playerId;
        String tokenId = "";

        try {
            if (!onlineMode) {
                // In online mode, we expect a payload with a Firebase token
                byte[] payload = packet.payload();

                // Check for a payload that is too short
                if (payload.length < 16 + Integer.BYTES) {
                    System.err.println("Authentication error: Received payload is too short for safe mode.");
                    sendFailure(packet.id(), "Authentication failed: Invalid payload.", packet.address(), packet.port());
                    return;
                }

                ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
                DataInputStream inStream = new DataInputStream(inputStream);

                // Read the UUID sent by the client
                byte[] uuidBytes = new byte[16];
                inStream.readFully(uuidBytes);
                playerId = bytesToUUID(uuidBytes);

                // Read the length of the Firebase token and then the token itself
                int sizeTokenId = inStream.readInt();
                byte[] byteTokenId = new byte[sizeTokenId];
                inStream.readFully(byteTokenId);
                tokenId = new String(byteTokenId, UTF_8);

                System.out.println("Verifying Firebase token...");
                Authentication.FirebaseTokenPayload p = Authentication.verify(tokenId);

                if (p.uid() == null || p.uid().isEmpty()) {
                    System.out.println("Invalid Firebase token. Rejecting.");
                    tokenId = ""; // Clear the token for the player object
                    sendFailure(packet.id(), "Authentication failed: Invalid token.", packet.address(), packet.port());
                    return;
                }
                System.out.println("Token verified for user ID: " + p.uid());

            } else {
                // In offline mode, we generate a random UUID and don't need a token
                System.out.println("Authentication skipped: Online mode is enabled.");
                playerId = UUID.randomUUID();
            }

            send("publicKey",packet.address(),packet.port(), UNENCRYPTED.getType(), true,false);

            System.out.println("User authenticated. Assigned UUID: " + playerId);

            Players.join(new PlayerData(
                    packet.address(),
                    "Test", // Placeholder name for now
                    playerId,
                    tokenId,
                    // Placeholder position
                    !tokenId.isEmpty() // isOnline check
            ));

            // Create the connection packet with the new UUID
            byte[] uuidPayload = ByteBuffer.allocate(16)
                    .putLong(playerId.getMostSignificantBits())
                    .putLong(playerId.getLeastSignificantBits())
                    .array();

            // Send a CONNECTION packet with the UUID to the client
            System.out.println("Sending connection confirmation with UUID: " + playerId);
            send(packet.id(), uuidPayload, packet.address(), packet.port(), CONNECTION.getType(), true, true);

        } catch (Exception e) {
            System.err.println("Authentication error:");
            e.printStackTrace();
            sendFailure(packet.id(), "Authentication failed.", packet.address(), packet.port());
        }
    }

    public static UUID bytesToUUID(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("UUID must be 16 bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(buffer.getLong(), buffer.getLong());
        System.out.println("Converted bytes to UUID: " + uuid);
        return uuid;
    }
}
