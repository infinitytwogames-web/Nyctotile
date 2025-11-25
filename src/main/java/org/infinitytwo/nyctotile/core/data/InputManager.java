package org.infinitytwo.nyctotile.core.data;

import org.infinitytwo.nyctotile.core.Window;

import java.util.HashMap;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    private final Window window;
    private final HashMap<Integer, Boolean> keyStates = new HashMap<>();
    private final HashMap<Integer, Boolean> lastKeyStates = new HashMap<>();
    private final HashMap<Integer, Boolean> mouseStates = new HashMap<>();
    private final HashMap<Integer, Boolean> lastMouseStates = new HashMap<>();
    
    public InputManager(Window window) {
        this.window = window;
    }
    
    public void update() {
        lastKeyStates.clear(); lastKeyStates.putAll(keyStates);
        for (int k = GLFW_KEY_SPACE; k <= GLFW_KEY_LAST; k++) {
            keyStates.put(k, glfwGetKey(window.getWindowHandle(), k) == GLFW_PRESS);
        }
        lastMouseStates.clear(); lastMouseStates.putAll(mouseStates);
        for (int b = GLFW_MOUSE_BUTTON_1; b <= GLFW_MOUSE_BUTTON_LAST; b++) {
            mouseStates.put(b, glfwGetMouseButton(window.getWindowHandle(), b) == GLFW_PRESS);
        }
    }
    
    public boolean isKeyPressed(int key) {
        return keyStates.getOrDefault(key, false);
    }
    public boolean isKeyJustPressed(int key) {
        return keyStates.getOrDefault(key, false) && !lastKeyStates.getOrDefault(key, false);
    }
    public boolean isMouseJustPressed(int button) {
        return mouseStates.getOrDefault(button, false) && !lastMouseStates.getOrDefault(button, false);
    }
}

