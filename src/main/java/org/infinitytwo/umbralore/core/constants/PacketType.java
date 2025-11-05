package org.infinitytwo.umbralore.core.constants;

/**
 * Defines the type of data contained within a network packet.
 * All packets share a standard, unencrypted network header (17 bytes):
 * <ul>
 * <li>[ Packet ID (int) ]</li>
 * <li>[ Nonce (int) ]</li>
 * <li>[ Expected Payload Length (int) ]</li>
 * <li>[ Packet Index (short) ]</li>
 * <li>[ Total Packets (short) ]</li>
 * <li>[ Packet Type (byte) ]</li>
 * </ul>
 */
public enum PacketType {
    /**
     * Payload Format:<br>
     * [ Blocks (byte[]) ] - The actual chunk data.
     */
    CHUNK((byte) 0),
    
    /**
     * Used for chat or simple console commands.<br>
     * Payload Format:<br>
     * [ Command (byte[] as UTF-8 string) ]
     */
    COMMAND((byte) 1),
    
    /**
     * Negative Acknowledgement. Used for reliable UDP to request re-send of missing pieces.<br>
     * Payload Format:<br>
     * [ Missing Packet Indexes (short[]) ]
     */
    NACK((byte) 2),
    
    /**
     * <h4>Not to be confused with {@code FAILURE}</h4>
     * Positive Acknowledgement. Used for reliable UDP to confirm a full packet was received.<br>
     * Payload Format: <i>[ 0, 0, 0, 0 ] (No payload, relies solely on the standard header)</i>
     */
    ACK((byte) 3),
    
    /**
     * Used for the initial authentication process.<br>
     * Payload Format:<br>
     * [ Token ID Size (int) ]<br>
     * [ Token ID (byte[] as string) ]
     */
    AUTHENTICATION((byte) 4),
    
    /**
     * Used for commands that also transfer arbitrary binary data (e.g., file transfer or complex operations).<br>
     * Payload Format:<br>
     * [ Command (byte[] as string) ]<br>
     * [ Arbitrary Data Payload (byte[]) ]
     */
    CMD_BYTE_DATA((byte) 5),
    
    /**
     * Used for messages that MUST be sent unencrypted, regardless of connection state (e.g., debug messages).<br>
     * Payload Format: <i>(Same as {@code COMMAND} but is explicitly flagged as unencrypted.)</i>
     */
    UNENCRYPTED((byte) 6),
    
    /**
     * <h4>Not to be confused with {@code NACK}</h4>
     * Used for errors/failures caused by command execution (e.g., wrong argument count).<br>
     * Payload Format:<br>
     * [ Error Message (byte[] as UTF-8 string) ]
     */
    FAILURE((byte) 7),
    
    /**
     * Primarily for connection handshake. Contains public key data.<br>
     * Payload Format:<br>
     * [ Public Key (byte[] as encoded RSA Key) ]
     */
    EXCHANGE((byte) 8),
    
    /**
     * Used to signal a successful connection after handshake and authentication.
     * Payload Format: [ Player UUID (byte[] as string) ]
     */
    CONNECTION((byte) 9),
    
    /**
     * Used to formally terminate a connection.<br>
     * Payload Format: <i>(No payload, relies solely on the standard header)</i>
     */
    DISCONNECTION((byte) 10)
    ;
    
    private final byte id;
    
    PacketType(byte id) {
        this.id = id;
    }
    
    public byte getByte() {
        return id;
    }
    
    public static PacketType fromId(byte id) {
        for (PacketType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PacketType ID: " + id);
    }
    
    public static PacketType valueOf(byte type) {
        return fromId(type);
    }
}