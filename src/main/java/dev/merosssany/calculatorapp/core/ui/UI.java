package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.RGB;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import org.lwjgl.assimp.AIVector2D;

public class UI {
    private Vector2D<Float> position;
    private float width;
    private float height;
    private RGB background;

    public UI(Vector2D<Float> position,float width, float height) {
        this.height = height;
        this.width = width;
        this.position = position;
    }

    public Vector2D<Float> getPosition() {
        return position;
    }

    public void setPosition(Vector2D<Float> position) {
        this.position = position;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
