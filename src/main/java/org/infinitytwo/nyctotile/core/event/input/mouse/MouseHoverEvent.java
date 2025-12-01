package org.infinitytwo.nyctotile.core.event.input.mouse;

import org.infinitytwo.nyctotile.core.event.Event;
import org.joml.Vector2i;

public class MouseHoverEvent extends Event {
    private Vector2i mousePosition;

    public MouseHoverEvent(Vector2i mousePosition) {
        this.mousePosition = mousePosition;
    }

    public MouseHoverEvent(int x, int y) {
        this.mousePosition = new Vector2i(x,y);
    }

    public MouseHoverEvent() {}

    public Vector2i getMousePosition() {
        return mousePosition;
    }


}
