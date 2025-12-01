package org.infinitytwo.nyctotile.core.manager;

import org.infinitytwo.nyctotile.core.exception.UnknownRegistryException;
import org.infinitytwo.nyctotile.core.ui.layout.Scene;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SceneManager {
    protected static final Map<String, Scene> scenes = new HashMap<>();
    protected static final Stack<Scene> ACTIVE_SCENES = new Stack<>();
    
    public static void register(String name, Scene scene) {
        scenes.put(name, scene);
    }
    
    // --- State Operations (New Features) ---
    
    /**
     * Pushes a new screen onto the stack, making it the top/active screen.
     * The new screen receives onOpen() notification.
     */
    public static void pushScreen(String name) {
        Scene scene = scenes.get(name);
        if (scene == null)
            throw new UnknownRegistryException("Screen \"" + name + "\" not found in registry.");
        
        if (ACTIVE_SCENES.contains(scene))
            return; // already active — ignore or bring to front
        
        try {
            Scene s = ACTIVE_SCENES.peek();
            if (s != null) s.stop();
        } catch (EmptyStackException ignored) {}
        
        ACTIVE_SCENES.push(scene);
        scene.open();
    }
    
    public static void setScreen(String name) {
        while (!ACTIVE_SCENES.isEmpty()) {
            ACTIVE_SCENES.pop().close();
        }
        pushScreen(name);
    }
    
    public static Scene getCurrent() {
        return ACTIVE_SCENES.isEmpty()? null : ACTIVE_SCENES.peek();
    }
    
    /**
     * Removes the top screen from the stack and notifies it that it's closing.
     */
    public static void popScreen() {
        if (ACTIVE_SCENES.isEmpty()) return;
        
        Scene closed = ACTIVE_SCENES.pop();
        closed.close();
        
        if (!ACTIVE_SCENES.isEmpty()) {
            // Assuming the Scene class has a 'start()' or 'resume()' method to undo 'stop()'.
            // Using 'open()' is often used for resume if a separate 'resume()' doesn't exist.
            // I recommend adding a dedicated 'resume()' method to the Scene class.
            ACTIVE_SCENES.peek().open();
        }
    }
    
    /**
     * Draws all active screens from bottom to top.
     * The bottom screen is usually the main HUD or Game view.
     */
    public static void draw() {
        // Iterate through the stack to draw all screens in order (bottom to top)
        for (Scene scene : ACTIVE_SCENES) {
            scene.draw();
        }
    }
}