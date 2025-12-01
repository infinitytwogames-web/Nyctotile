package org.infinitytwo.nyctotile.core.manager;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.Item;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.renderer.FontRenderer;
import org.infinitytwo.nyctotile.core.ui.layout.Scene;
import org.infinitytwo.nyctotile.core.ui.component.ItemHolder;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public class Mouse {
    private static ItemHolder itemHolder;
    private static double lastX, lastY;
    private static double deltaX, deltaY;
    private static boolean first = true;
    private static Window window;

    public static void setWindow(Window window) {
        Mouse.window = window;
    }

    public static Window getWindow() {
        return window;
    }

    public static void init(TextureAtlas a, Scene scene, int index, FontRenderer renderer, Window window) {
        itemHolder = new ItemHolder(a, scene, index, renderer);
        Mouse.window = window;

        itemHolder.setSize(64,64);
    }

    public static void update() {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(window.getWindow(), x, y);

        if (first) {
            lastX = x[0];
            lastY = y[0];
            first = false;
        }

        deltaX = x[0] - lastX;
        deltaY = y[0] - lastY;

        lastX = x[0];
        lastY = y[0];
    }
    
    public static void setDeltaX(double newDeltaX) {
        // 1. Get the current position
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(window.getWindow(), x, y);
        
        // 2. Set the last tracked position to the current position
        // This ensures the NEXT delta calculation (x[0] - lastX) is zero.
        Mouse.lastX = x[0];
        
        // 3. Set the delta itself to the desired value (usually 0 when resetting)
        Mouse.deltaX = newDeltaX;
    }
    
    public static void setDeltaY(double newDeltaY) {
        // 1. Get the current position
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(window.getWindow(), x, y);
        
        // 2. Set the last tracked position to the current position
        Mouse.lastY = y[0];
        
        // 3. Set the delta itself to the desired value (usually 0 when resetting)
        Mouse.deltaY = newDeltaY;
    }
    
    public static Vector2i getCurrentPosition() {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(window.getWindowHandle(), x, y);

        return new Vector2i((int) x[0], (int) y[0]);
    }

    public static double getDeltaX() { return deltaX; }
    public static double getDeltaY() { return deltaY; }

    public static void setItem(Item item) {
        itemHolder.setItem(item);
    }

    public static RGBA getForegroundColor() {
        return itemHolder.getForegroundColor();
    }

    public static void setForegroundColor(RGBA foregroundColor) {
        itemHolder.setForegroundColor(foregroundColor);
    }

    public static int getTextureIndex() {
        return itemHolder.getTextureIndex();
    }

    public static void setTextureIndex(int textureIndex) {
        itemHolder.setTextureIndex(textureIndex);
    }

    public static TextureAtlas getAtlas() {
        return itemHolder.getAtlas();
    }

    public static void setAtlas(TextureAtlas atlas) {
        itemHolder.setAtlas(atlas);
    }

    public static Item getItem() {
        return itemHolder.getItem();
    }

    public static void setCount(int count) {
        itemHolder.setCount(count);
    }

    public static void damage(int amount) {
        itemHolder.damage(amount);
    }

    public static void repair(int amount) {
        itemHolder.repair(amount);
    }

    public static boolean isBroken() {
        return itemHolder.isBroken();
    }

    public static boolean isStackable() {
        return itemHolder.isStackable();
    }

    public static void renderUsing(TextureAtlas atlas, int index) {
        itemHolder.renderUsing(atlas, index);
    }

    public static Scene getScreen() {
        return itemHolder.getScreen();
    }

    public static void draw() {
        if (itemHolder == null) return;
        itemHolder.setOffset(Display.transformWindowToVirtual(window,getCurrentPosition()));
        itemHolder.draw();
    }
}
