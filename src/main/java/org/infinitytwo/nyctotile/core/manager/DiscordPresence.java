package org.infinitytwo.nyctotile.core.manager;

import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

@Deprecated
public class DiscordPresence {
    public static void startRPC() {
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();

        DiscordRPC.discordInitialize("1387184917133267066", handlers, true);

        DiscordRichPresence presence = new DiscordRichPresence.Builder("Playing My Game")
                .setDetails("Developing My Game...")
                .setStartTimestamps(System.currentTimeMillis())
                .build();

        DiscordRPC.discordUpdatePresence(presence);
    }
}
