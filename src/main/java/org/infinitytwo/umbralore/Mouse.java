package org.infinitytwo.umbralore;

import org.infinitytwo.umbralore.debug.Main;
import org.infinitytwo.umbralore.item.Item;
import org.infinitytwo.umbralore.registry.ItemRegistry;
import org.infinitytwo.umbralore.ui.component.ItemHolder;
import org.infinitytwo.umbralore.ui.position.Anchor;
import org.infinitytwo.umbralore.ui.position.Pivot;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

public class Mouse {
    private final ItemHolder itemHolder;
    private double lastX, lastY;
    private double deltaX, deltaY;
    private boolean first = true;
    private final Window window;

    public Mouse(Window window) {
        this.window = window;
        itemHolder = new ItemHolder(ItemRegistry.getTextureAtlas(), Main.getCurrentScreen(), 0, Main.getCurrentScreen().getFontRenderer());

        itemHolder.setPosition(new Anchor(0,0), new Pivot(0,0));
    }

    public void update() {
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

    public Vector2i getCurrentPosition() {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window.getWindowHandle(), x, y);

        return new Vector2i((int) x[0], (int) y[0]);
    }

    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }

    public void setItem(Item item) {
        itemHolder.setItem(item);
    }

    public void draw() {
        itemHolder.setOffset(getCurrentPosition());
        itemHolder.draw();
    }

    public Item getItem() {
        return itemHolder.getItem();
    }
}
