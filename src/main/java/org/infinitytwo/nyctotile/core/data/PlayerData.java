package org.infinitytwo.nyctotile.core.data;

import com.esotericsoftware.kryonet.Connection;

public record PlayerData(String name, String id, String token, boolean authenticated, Connection connection) {
    
    public static PlayerData shell(String name) {
        return new PlayerData(name, "", "", false, null);
    }
}
