package dev.merosssany.calculatorapp.core.io;

import dev.merosssany.calculatorapp.core.RGBA;
import dev.merosssany.calculatorapp.core.Window;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.core.ui.InteractableUI;
import dev.merosssany.calculatorapp.core.ui.UI;
import dev.merosssany.calculatorapp.logging.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public abstract class HoverEventRegister {
    private static Window window;
    private static final ArrayList<UI> listeners = new ArrayList<>();
    private static final Logger logger = new Logger("Hover Event");
    private static final Thread event = new Thread("Hover Event");

    public static void registerUI(UI ui) {
        logger.log("Registering UI Element:", ui.getName());
        if (!listeners.contains(ui)) {
            listeners.add(ui);
        }
    }

    public static void update() {
        double[] cursorPositionX = new double[1];
        double[] cursorPositionY = new double[1];
        GLFW.glfwGetCursorPos(window.getWindow(), cursorPositionX, cursorPositionY);
        double mouseX = cursorPositionX[0];
        double mouseY = cursorPositionY[0];
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        float normalizedMouseX = (float) ((2.0 * mouseX) / windowWidth - 1.0);
        float normalizedMouseY = (float) (1.0 - (2.0 * mouseY) / windowHeight);

        Vector2D<Float> normalizedMousePos = new Vector2D<>(normalizedMouseX, normalizedMouseY);
    }

    public static void init(Window window) {
        HoverEventRegister.window = window;
    }
}
