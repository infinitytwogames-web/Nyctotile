package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.exception.UnknownRegistryException;
import org.infinitytwo.umbralore.core.ui.display.Screen;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ScreenManager {
    public static final Map<String, Screen> screens = new HashMap<>();
    private static final Stack<Screen> activeScreens = new Stack<>();
    public static void register(String name, Screen screen) {
        screens.put(name, screen);
    }

    // --- State Operations (New Features) ---

    /**
     * Pushes a new screen onto the stack, making it the top/active screen.
     * The new screen receives onOpen() notification.
     */
    public static void pushScreen(String name) {
        Screen screen = screens.get(name);
        if (screen == null)
            throw new UnknownRegistryException("Screen \"" + name + "\" not found in registry.");
        
        if (activeScreens.contains(screen))
            return; // already active — ignore or bring to front
        
        activeScreens.push(screen);
        screen.open();
    }
    
    public static void setScreen(String name) {
        while (!activeScreens.isEmpty()) {
            activeScreens.pop().close();
        }
        pushScreen(name);
    }
    
    public static Screen getCurrent() {
        return activeScreens.isEmpty() ? null : activeScreens.peek();
    }
    
    /**
     * Removes the top screen from the stack and notifies it that it's closing.
     */
    public static void popScreen() {
        if (activeScreens.isEmpty()) return;

        Screen closed = activeScreens.pop();
        closed.close();
    }

    /**
     * Draws all active screens from bottom to top.
     * The bottom screen is usually the main HUD or Game view.
     */
    public static void draw() {
        // Iterate through the stack to draw all screens in order (bottom to top)
        for (Screen screen : activeScreens) {
            screen.draw();
        }
    }

    /**
     * Delegates input to the top-most screen for Z-order priority.
     */
    public static void handleInput() {
        if (!activeScreens.isEmpty()) {
            Screen top = activeScreens.peek();
            // Assuming your Screen class has a method to handle input events
            // top.handleInput();
        }
    }
}