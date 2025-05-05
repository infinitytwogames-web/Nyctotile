package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.RGBA;
import dev.merosssany.calculatorapp.core.Window;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.position.Vector2D;

import java.awt.*;
import java.io.InputStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;
import org.lwjgl.nanovg.*;

public class Button extends InteractableUI {
    private boolean isPressed = false;
    private Runnable mouseClick;
    private String text;
    private final float padding = 0;

    public Button(UIVector2Df position, float width, float height, RGBA background, Window window) {
        super(position, width, height, background, window);
    }

    public Button(Runnable onMouseRightClick, UIVector2Df position, float width, float height, RGBA background, Window window) {
        super(position, width, height, background, window);
        this.mouseClick = onMouseRightClick;
    }

    public boolean isPressed() {
        return isPressed;
    }

    @Override
    public void onMouseRightClick() {
        isPressed = true;
        if (mouseClick != null) mouseClick.run();
    }

    @Override
    public void draw() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (backgroundRGBA != null) {
            glColor4f(backgroundRGBA.getRed(), backgroundRGBA.getGreen(), backgroundRGBA.getBlue(), backgroundRGBA.getAlpha());
        } else {
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Default to opaque white
        }
        Vector2D<Float> position = this.getPosition();

        float topLeftX = position.getX();
        float topLeftY = position.getY();
        float widthNDC = this.getWidth();   // Assuming width is in NDC scale
        float heightNDC = this.getHeight(); // Assuming height is in NDC scale

        glBegin(GL_QUADS);
        glVertex2f(topLeftX, topLeftY);             // Top-left
        glVertex2f(topLeftX + widthNDC, topLeftY);      // Top-right
        glVertex2f(topLeftX + widthNDC, topLeftY - heightNDC); // Bottom-right (assuming +Y is up in NDC)
        glVertex2f(topLeftX, topLeftY - heightNDC);      // Bottom-left
        glEnd();

        glDisable(GL_BLEND);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float getPadding() {
        return padding;
    }
}
