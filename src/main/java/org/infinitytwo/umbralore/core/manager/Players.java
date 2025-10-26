package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.data.PlayerData;

import java.net.InetAddress;
import java.util.*;

public final class Players {
    private static final List<PlayerData> PLAYER_DATA = Collections.synchronizedList(new ArrayList<>());

    public static PlayerData getPlayerByAddress(InetAddress address) {
        for (PlayerData playerData : PLAYER_DATA) {
            if (playerData.address.equals(address)) return playerData;
        }
        return null;
    }

    public static PlayerData getPlayerById(UUID id) {
        for (PlayerData playerData : PLAYER_DATA) {
            if (Objects.equals(playerData.id, id)) return playerData;
        }
        return null;
    }

    public static void join(PlayerData playerData) {
        PLAYER_DATA.add(playerData);
    }
}
