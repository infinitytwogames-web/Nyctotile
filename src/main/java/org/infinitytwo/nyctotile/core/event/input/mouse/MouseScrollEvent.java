package org.infinitytwo.nyctotile.core.event.input.mouse;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.event.Event;

public class MouseScrollEvent extends Event {
    public final Window window;
    public final int x,y;

    public MouseScrollEvent(Window window, int x, int y) {
        this.window = window;
        this.x = x;
        this.y = y;
    }
}
