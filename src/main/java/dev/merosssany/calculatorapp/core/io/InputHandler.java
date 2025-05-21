package dev.merosssany.calculatorapp.core.io;

import dev.merosssany.calculatorapp.core.event.EventBus;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.core.render.Window;
import dev.merosssany.calculatorapp.core.event.KeyPressEvent;
import dev.merosssany.calculatorapp.core.event.MouseButtonEvent;
import dev.merosssany.calculatorapp.core.logging.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

public class InputHandler {
    private final Window window;
    private GLFWKeyCallback glfwKeyCallback;
    private GLFWMouseButtonCallback glfwMouseButtonCallback;
    private final Logger logger = new Logger("InputHandler");

    public InputHandler(Window window) {
        this.window = window;
    }

    public void init() {
        glfwKeyCallback = GLFW.glfwSetKeyCallback(window.getWindow(), (windowHandle, key, scancode, action, mods) -> {
            logger.info("Posting KeyPressEvent");
            EventBus.post(new KeyPressEvent(key, action));
        });

        glfwMouseButtonCallback = GLFW.glfwSetMouseButtonCallback(window.getWindow(), (windowHandle, button, action, mods) -> {
            logger.info("Posting MouseButtonEvent");
            EventBus.post(new MouseButtonEvent(button, action));
        });
    }

    public void cleanup() {
        if (glfwKeyCallback != null) glfwKeyCallback.free();
        if (glfwMouseButtonCallback != null) glfwMouseButtonCallback.free();
    }

    public Vector2D<Float> getMousePosition() {
        double[] cursorPositionX = new double[1];
        double[] cursorPositionY = new double[1];
        GLFW.glfwGetCursorPos(window.getWindow(), cursorPositionX, cursorPositionY);
        double mouseX = cursorPositionX[0];
        double mouseY = cursorPositionY[0];
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        float normalizedMouseX = (float) ((2.0 * mouseX) / windowWidth - 1.0);
        float normalizedMouseY = (float) (1.0 - (2.0 * mouseY) / windowHeight);

        return new Vector2D<>(normalizedMouseX,normalizedMouseY);
    }
}
