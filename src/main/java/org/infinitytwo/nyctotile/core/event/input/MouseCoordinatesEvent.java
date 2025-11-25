package org.infinitytwo.nyctotile.core.event.input;

import org.infinitytwo.nyctotile.core.event.Event;

public class MouseCoordinatesEvent extends Event {
    private final float x;
    private final float y;

    public MouseCoordinatesEvent(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}