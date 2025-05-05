package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.RGBA;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.logging.Logger;

import static org.lwjgl.opengl.GL11.*;

public class UI {
    private UIVector2Df position;
    private final float width;
    private final float height;
    protected RGBA backgroundRGBA;
    private Vector2D<Float> end;
    private final Logger logger = new Logger("UI Handler");
    private final String name;

    public void setBackgroundColor(RGBA color) {
        backgroundRGBA = color;
    }

    public RGBA getBackgroundColor() {
        return backgroundRGBA;
    }

    public UI(String name,UIVector2Df position, float width, float height, RGBA background) {
        this.height = height;
        this.width = width;
        this.position = position;
        this.backgroundRGBA = background;
        this.name = name;

        float topLeftX = position.getX();
        float topLeftY = position.getY();

        this.end = new Vector2D<>(topLeftX + width, topLeftY - height);
    }

    public Vector2D<Float> getPosition() {
        return position;
    }

    public void setPosition(UIVector2Df position) {
        this.position = position;
        float topLeftX = position.getX();
        float topLeftY = position.getY();

        this.end = new Vector2D<>(topLeftX + width, topLeftY - height);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Vector2D<Float> getEnd() {
        return end;
    }

    public Logger getLogger() {
        return logger;
    }

    // New method to change background color using individual components
    public void changeBackgroundColor(float red, float green, float blue, float alpha) {
        this.backgroundRGBA = new RGBA(red, green, blue, alpha);
    }

    public void draw() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (backgroundRGBA != null) {
            glColor4f(backgroundRGBA.getRed(), backgroundRGBA.getGreen(), backgroundRGBA.getBlue(), backgroundRGBA.getAlpha());
        } else {
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Default to opaque white
        }

        float topLeftX = position.getX();
        float topLeftY = position.getY();
        float widthNDC = width;   // Assuming width is in NDC scale
        float heightNDC = height; // Assuming height is in NDC scale

        glBegin(GL_QUADS);
        glVertex2f(topLeftX, topLeftY);             // Top-left
        glVertex2f(topLeftX + widthNDC, topLeftY);      // Top-right
        glVertex2f(topLeftX + widthNDC, topLeftY - heightNDC); // Bottom-right (assuming +Y is up in NDC)
        glVertex2f(topLeftX, topLeftY - heightNDC);      // Bottom-left
        glEnd();

        glDisable(GL_BLEND); // Consider managing this outside the draw method for efficiency
    }

    public String getName() {
        return name;
    }
}