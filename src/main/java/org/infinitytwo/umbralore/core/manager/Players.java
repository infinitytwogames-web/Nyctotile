package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.data.PlayerData;
import org.infinitytwo.umbralore.core.entity.Player;
import org.infinitytwo.umbralore.core.network.NetworkThread;
import org.infinitytwo.umbralore.core.renderer.Camera;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Players {
    // 1. Storage for PlayerData, keyed by UUID (fast game-logic lookup)
    private static final Map<UUID, PlayerData> PLAYER_DATA_BY_ID = new ConcurrentHashMap<>();
    
    // 2. Storage for PlayerData, keyed by "IP:Port" (fast network lookup)
    private static final Map<String, PlayerData> PLAYER_DATA_BY_ADDRESS = new ConcurrentHashMap<>();
    
    // 3. Storage for Player Entities, keyed by PlayerData instance
    private static final Map<PlayerData, Player> PLAYER_ENTITY = new ConcurrentHashMap<>();
    
    /** Helper to create a unique network key. */
    private static String getAddressKey(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }
    
    // --- Lookups ---
    
    /** Retrieves PlayerData using the unique network address and port. */
    public static PlayerData getPlayerByAddress(InetAddress address, int port) {
        return PLAYER_DATA_BY_ADDRESS.get(getAddressKey(address, port));
    }
    
    /** Retrieves PlayerData using the unique network packet for abbreviation. */
    public static PlayerData getPlayerByAddress(NetworkThread.Packet packet) {
        // Correctly delegates both address and port to the primary lookup method.
        return getPlayerByAddress(packet.address(), packet.port());
    }
    
    /** Retrieves PlayerData using the player's unique ID. */
    public static PlayerData getPlayerById(UUID id) {
        return PLAYER_DATA_BY_ID.get(id);
    }
    
    /** Retrieves the Player entity associated with the PlayerData. */
    public static Player getPlayer(PlayerData data) {
        return PLAYER_ENTITY.get(data);
    }
    
    // --- Join/Leave ---
    
    /** Handles player joining, creating a new Player entity. */
    public static void join(PlayerData playerData) {
        // Store the data in both lookup maps
        PLAYER_DATA_BY_ID.put(playerData.id(), playerData);
        PLAYER_DATA_BY_ADDRESS.put(getAddressKey(playerData.address(), playerData.port()), playerData);
        
        // Initialize the Player Entity (assuming World and Player constructor are correct)
        Player newPlayer = new Player(playerData, World.getSpawnLocation().dimension(), new Camera(), null);
        PLAYER_ENTITY.put(playerData, newPlayer);
        newPlayer.setPosition(World.getSpawnLocation().position());
    }
    
    /** Handles player joining, supplying an existing Player entity (e.g., from save data). */
    public static void join(PlayerData playerData, Player player) {
        // Store the data in both lookup maps
        PLAYER_DATA_BY_ID.put(playerData.id(), playerData);
        PLAYER_DATA_BY_ADDRESS.put(getAddressKey(playerData.address(), playerData.port()), playerData);
        
        // Store the provided Player Entity
        PLAYER_ENTITY.put(playerData, player);
    }
}