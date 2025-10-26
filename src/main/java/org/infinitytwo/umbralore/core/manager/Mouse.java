package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.Display;
import org.infinitytwo.umbralore.core.RGBA;
import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.Item;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.renderer.FontRenderer;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.component.ItemHolder;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

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

    public static void init(TextureAtlas a, Screen screen, int index, FontRenderer renderer, Window window) {
        itemHolder = new ItemHolder(a, screen, index, renderer);
        Mouse.window = window;

        itemHolder.setSize(64,64);
    }

    public static void update() {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window.getWindowHandle(), x, y);

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

    public static Vector2i getCurrentPosition() {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window.getWindowHandle(), x, y);

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

    public static Screen getScreen() {
        return itemHolder.getScreen();
    }

    public static void draw() {
        if (itemHolder == null) return;
        itemHolder.setOffset(Display.transformWindowToVirtual(window,getCurrentPosition()));
        itemHolder.draw();
    }
}
