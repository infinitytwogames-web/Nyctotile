package dev.merosssany.calculatorapp.core.event;

import dev.merosssany.calculatorapp.core.position.Vector2D;

public class MouseHoverEvent extends Event {
    private Vector2D<Float> mousePosition;

    public MouseHoverEvent(Vector2D<Float> mousePosition) {
        this.mousePosition = mousePosition;
    }

    public MouseHoverEvent(float x, float y) {
        this.mousePosition = new Vector2D<>(x,y);
    }

    public MouseHoverEvent() {}

    public Vector2D<Float> getMousePosition() {
        return mousePosition;
    }


}
