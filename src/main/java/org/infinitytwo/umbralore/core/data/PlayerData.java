package org.infinitytwo.umbralore.core.data;

import java.net.InetAddress;
import java.util.UUID;

public record PlayerData(InetAddress address, int port, String name, UUID id, String token, boolean authenticated) {
    
    public static PlayerData shell(String name) {
        return new PlayerData(null, 0, name, UUID.randomUUID(), "", false);
    }
}
