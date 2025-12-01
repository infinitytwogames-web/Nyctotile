package org.infinitytwo.nyctotile.core.event.input.keyboard;

import org.infinitytwo.nyctotile.core.event.Event;

public class KeyPressEvent extends Event {
    public final int key;
    public final int action;
    public final int mods;

    public KeyPressEvent(int key, int action, int mods) {
        this.key = key;
        this.action = action;
        this.mods = mods;
    }

    public int getKey() {
        return key;
    }

    public int getAction() {
        return action;
    }
}