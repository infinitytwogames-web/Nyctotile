package org.infinitytwo.nyctotile.core.ui.layout;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.builtin.Background;
import org.infinitytwo.nyctotile.core.ui.builtin.Rectangle;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.joml.Vector2i;

public class GUI extends Scene {
    protected int width, height;
    protected Rectangle gui; // GUI background
    protected Background background; // Darkens the background

    public GUI(UIBatchRenderer renderer, Window window) {
        super(renderer, window);

        gui = new Rectangle(renderer);

        background = new Background(renderer);
        background.setBackgroundColor(0,0,0,0.25f);
    }

    @Override
    protected void drawUIs() {
        background.draw();
        gui.draw();
        super.drawUIs();
    }

    public void setPosition(Anchor anchor, Pivot pivot) {
        gui.setPosition(anchor, pivot);
    }

    public void setPosition(Anchor anchor, Pivot pivot, Vector2i offset) {
        gui.setPosition(anchor, pivot, offset);
    }

    public void setOffset(Vector2i offset) {
        gui.setOffset(offset);
    }

    public void setOffset(int x, int y) {
        gui.setOffset(x, y);
    }

    public void addOffset(Vector2i v) {
        gui.addOffset(v);
    }

    public void setAnchor(Anchor anchor) {
        gui.setAnchor(anchor);
    }

    public void setAnchor(float x, float y) {
        gui.setAnchor(x, y);
    }

    public void setPivot(float x, float y) {
        gui.setPivot(x, y);
    }

    public void setPivot(Pivot pivot) {
        gui.setPivot(pivot);
    }

    public Anchor getAnchor() {
        return gui.getAnchor();
    }

    public Pivot getPivot() {
        return gui.getPivot();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RGBA getBackgroundColor() {
        return gui.getBackgroundColor();
    }

    public void setBackgroundColor(RGBA backgroundColor) {
        gui.setBackgroundColor(backgroundColor);
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        gui.setBackgroundColor(r, g, b, a);
    }

    public void setSize(Vector2i size) {
        gui.setSize(size);
    }

    public void setSize(int width, int height) {
        gui.setSize(width, height);
    }

    public void setSize(int same) {
        gui.setSize(same);
    }

    public void set(UI ui) {
        gui.set(ui);
    }

    public void setWidth(int width) {
        gui.setWidth(width);
        this.width = width;
    }

    public void setHeight(int height) {
        gui.setHeight(height);
        this.height = height;
    }

    public Vector2i getOffset() {
        return gui.getOffset();
    }

    @Override
    public int register(UI ui) {
        ui.setParent(gui);
        return super.register(ui);
    }

    @Override
    @SubscribeEvent
    public void onMouseClicked(MouseButtonEvent e) {
        super.onMouseClicked(e);
    }

    public static class Builder {
        private final GUI gui;

        public Builder(UIBatchRenderer renderer, Window window) {
            this.gui = new GUI(renderer,window);
        }

        public void setPivot(Pivot pivot) {
            gui.setPivot(pivot);
        }

        public void setOffset(Vector2i offset) {
            gui.setOffset(offset);
        }

        public void setAnchor(float x, float y) {
            gui.setAnchor(x, y);
        }

        public void set(UI ui) {
            gui.set(ui);
        }

        public void setWidth(int width) {
            gui.setWidth(width);
        }

        public void addOffset(Vector2i v) {
            gui.addOffset(v);
        }

        public void setPosition(Anchor anchor, Pivot pivot) {
            gui.setPosition(anchor, pivot);
        }

        public void setPosition(Anchor anchor, Pivot pivot, Vector2i offset) {
            gui.setPosition(anchor, pivot, offset);
        }

        public void setSize(int width, int height) {
            gui.setSize(width, height);
        }

        public void setAnchor(Anchor anchor) {
            gui.setAnchor(anchor);
        }

        public void setBackgroundColor(float r, float g, float b, float a) {
            gui.setBackgroundColor(r, g, b, a);
        }

        public void setOffset(int x, int y) {
            gui.setOffset(x, y);
        }

        public void setPivot(float x, float y) {
            gui.setPivot(x, y);
        }

        public void setSize(Vector2i size) {
            gui.setSize(size);
        }

        public void setSize(int same) {
            gui.setSize(same);
        }

        public void setHeight(int height) {
            gui.setHeight(height);
        }

        public void setBackgroundColor(RGBA backgroundColor) {
            gui.setBackgroundColor(backgroundColor);
        }
    }
}
