package org.infinitytwo.nyctotile.core.event.state;

import org.infinitytwo.nyctotile.core.event.Event;
import org.infinitytwo.nyctotile.core.Window;

public class WindowResizedEvent extends Event {
    public int width, height;
    public Window window;

    public WindowResizedEvent(int width, int height, Window window) {
        this.width = width;
        this.height = height;
        this.window = window;
    }

    public WindowResizedEvent(Window window) {
        this.width = window.getWidth();
        this.height = window.getHeight();
        this.window = window;
    }
}
