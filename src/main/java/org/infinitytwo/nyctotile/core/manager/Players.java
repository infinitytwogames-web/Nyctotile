package org.infinitytwo.nyctotile.core.manager;

import com.esotericsoftware.kryonet.Connection;
import org.infinitytwo.nyctotile.core.data.PlayerData;
import org.infinitytwo.nyctotile.core.entity.Player;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Players {
    // 1. Storage for PlayerData, keyed by UUID (fast game-logic lookup)
    private static final Map<String, PlayerData> PLAYER_DATA_BY_ID = new ConcurrentHashMap<>();
    
    // 2. Storage for PlayerData, keyed by "IP:Port" (legacy/redundant, can be removed later)
    private static final Map<PlayerData, Player> PLAYER_ENTITY = new ConcurrentHashMap<>();
    
    // 4. NEW: Storage for PlayerData, keyed by KryoNet Connection object
    private static final Map<Connection, PlayerData> PLAYER_DATA_BY_CONNECTION = new ConcurrentHashMap<>();
    
    public static PlayerData getPlayerByConnection(Connection connection) {
        return PLAYER_DATA_BY_CONNECTION.get(connection);
    }
    
    /** Helper to create a unique network key. */
    private static String getAddressKey(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }
    
    // --- Lookups ---
    
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
        // Store the data in all lookup maps
        PLAYER_DATA_BY_ID.put(playerData.id(), playerData);
        PLAYER_DATA_BY_CONNECTION.put(playerData.connection(), playerData);
        
        Player newPlayer = new Player(playerData, World.getSpawnLocation().dimension(), null);
        newPlayer.setPosition(World.getSpawnLocation().position());
        newPlayer.adjust();
        newPlayer.setUUID(UUID.randomUUID());
        EntityManager.put(newPlayer);
        
        PLAYER_ENTITY.put(playerData, newPlayer);
        newPlayer.setPosition(World.getSpawnLocation().position());
    }
    
    /** Handles player joining, supplying an existing Player entity (e.g., from save data). */
    @Deprecated
    public static void join(PlayerData playerData, Player player) {
        // Store the data in both lookup maps
        PLAYER_DATA_BY_ID.put(playerData.id(), playerData);
        PLAYER_ENTITY.put(playerData, player);
    }
    
    public static PlayerData leave(Connection connection) {
        PlayerData data = PLAYER_DATA_BY_CONNECTION.remove(connection);
        
        if (data != null) {
            PLAYER_DATA_BY_ID.remove(data.id());
            PLAYER_ENTITY.remove(data);
            return data;
        }
        return null;
    }
}