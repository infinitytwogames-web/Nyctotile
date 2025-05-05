package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.EventBus;
import dev.merosssany.calculatorapp.core.RGBA;
import dev.merosssany.calculatorapp.core.Window;
import dev.merosssany.calculatorapp.core.event.*;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.logging.Logger;
import org.lwjgl.glfw.GLFW;

public class InteractableUI extends UI{
    private final Window window;
    private final Logger logger = new Logger("Interactable UI");
    private RGBA original = this.getBackgroundColor();

    public InteractableUI(UIVector2Df position, float width, float height, RGBA background, Window window) {
        super("Interactable UI", position, width, height, background);
        this.window = window;

        EventBus.register(this);
    }

    @SubscribeEvent
    private void onEventFired(MouseButtonEvent e) {
        logger.log("Got event");
        if (e != null) {
            int key = e.getButton();
            int action = e.getAction();

            if (key == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (isInRange()) onMouseRightClick();
            }
        }
    }

    public void setInteractableBackgroundColor(RGBA color) {
        this.original = color;
        setBackgroundColor(color);
    }

    public void changeInteractableBackgroundColor(float r, float g, float b, float a) {
        this.original = new RGBA(r,g,b,a);
        changeBackgroundColor(r,g,b,a);
    }

    private boolean isInRange() {
        double[] cursorPositionX = new double[1];
        double[] cursorPositionY = new double[1];
        GLFW.glfwGetCursorPos(window.getWindow(), cursorPositionX, cursorPositionY);
        double mouseX = cursorPositionX[0];
        double mouseY = cursorPositionY[0];

        int windowWidth = window.getWidth(); // Assuming you have a getWidth() method in your Window class
        int windowHeight = window.getHeight(); // Assuming you have a getHeight() method

// Convert mouse X to normalized coordinates (-1 to 1)
        float normalizedMouseX = (float) ((2.0 * mouseX) / windowWidth - 1.0);

// Convert mouse Y to normalized coordinates (1 to -1, assuming UI y-axis goes down)
        float normalizedMouseY = (float) (1.0 - (2.0 * mouseY) / windowHeight);

        Vector2D<Float> normalizedMousePos = new Vector2D<>(normalizedMouseX, normalizedMouseY);
//        logger.log(this.getPosition(),normalizedMousePos, this.getEnd());
        if (this.getPosition().isVectorPointIncludedIn(normalizedMousePos, this.getEnd())) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    private void mouseHoverEvent(MouseHoverEvent e) {
        if (isInRange()) onMouseHover();
        else mouseNotHovering();
    }

    public void onMouseHover() {
//        getLogger().log("Mouse Hover");
        changeBackgroundColor(original.getRed(),original.getGreen(),original.getBlue(),original.getAlpha() - 0.3f);
    }

    public void mouseNotHovering() {
//        getLogger().log("Mouse Not Hovering");
        changeBackgroundColor(original.getRed(),original.getGreen(),original.getBlue(),original.getAlpha());
    }

    public void onMouseRightClick() {
        getLogger().log("Button Pressed");
    }

    public RGBA getOriginal() {
        return original;
    }
}
