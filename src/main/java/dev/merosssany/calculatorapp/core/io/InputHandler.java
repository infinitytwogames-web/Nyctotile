package dev.merosssany.calculatorapp.core.io;

import dev.merosssany.calculatorapp.core.EventBus;
import dev.merosssany.calculatorapp.core.Window;
import dev.merosssany.calculatorapp.core.event.KeyPressEvent;
import dev.merosssany.calculatorapp.core.event.MouseButtonEvent;
import dev.merosssany.calculatorapp.logging.Logger;
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
}
