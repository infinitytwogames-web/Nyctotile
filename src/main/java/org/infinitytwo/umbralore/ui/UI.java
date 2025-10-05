package org.infinitytwo.umbralore.ui;

import org.infinitytwo.umbralore.Display;
import org.infinitytwo.umbralore.RGBA;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.input.ImageButton;
import org.infinitytwo.umbralore.ui.position.Anchor;
import org.infinitytwo.umbralore.ui.position.Pivot;
import org.joml.Vector2i;

public abstract class UI {
    protected final UIBatchRenderer renderer;
    protected RGBA backgroundColor = new RGBA();

    protected int width = 0;
    protected int height = 0;

    protected boolean hovering = false;

    protected Anchor anchor = new Anchor();
    protected Pivot pivot = new Pivot(0,0);
    protected Vector2i offset = new Vector2i();
    protected UI parent;

    public UI(UIBatchRenderer renderer) {
        this.renderer = renderer;
    }

    public boolean isHovering() {
        return hovering;
    }

    public void setHovering(boolean hovering) {
        this.hovering = hovering;
    }

    public Vector2i getPosition() {
        int xa, ya;
        Vector2i o;

        if (parent == null) {
            xa = (int) (Display.width * anchor.x);
            ya = (int) (Display.height * anchor.y);
            o = offset;
        } else {
            xa = (int) (parent.width * anchor.x);
            ya = (int) (parent.height * anchor.y);
            o = new Vector2i(offset).add(parent.getPosition());
        }

        int xp = (int) (width * pivot.x());
        int yp = (int) (height * pivot.y());

        int x = xa + xp;
        int y = ya + yp;
        return new Vector2i(x+o.x,y+o.y);
    }

    public void setPosition(Anchor anchor, Pivot pivot) {
        setAnchor(anchor);
        setPivot(pivot);
    }

    public void setPosition(Anchor anchor, Pivot pivot, Vector2i offset) {
        setPosition(anchor,pivot);
        setOffset(offset);
    }

    public Vector2i getOffset() {
        return new Vector2i(offset);
    }

    public void setOffset(Vector2i offset) {
        this.offset.set(offset);
    }

    public void setOffset(int x, int y) {
        offset.set(x,y);
    }

    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    public void setPivot(Pivot pivot) {
        this.pivot = pivot;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public Pivot getPivot() {
        return pivot;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RGBA getBackgroundColor() {
        return backgroundColor;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public UI getParent() {
        return parent;
    }

    public void setParent(UI parent) {
        this.parent = parent;
    }

    public void setBackgroundColor(RGBA backgroundColor) {
        this.backgroundColor.set(backgroundColor);
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        this.backgroundColor.set(r,g,b,a);
    }

    public void draw() { // Can be used as "update"
        renderer.queue(this);
    }

    public Vector2i getEnd() {
        return getPosition().add(width,height);
    }

    public void addOffset(Vector2i v) {
        offset.add(v);
    }

    public void setSize(Vector2i size) {
        setSize(size.x,size.y);
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    protected void setSize(int same) {
        setSize(same,same);
    }

    public abstract void onMouseClicked(MouseButtonEvent e);
    public abstract void onMouseHover(MouseHoverEvent e);
    public abstract void onMouseHoverEnded();
    public abstract void cleanup();


    public void set(UI ui) {
        setSize(ui.getWidth(),ui.getHeight());
        setBackgroundColor(ui.getBackgroundColor());
        setPosition(ui.getAnchor(),ui.getPivot(),ui.getOffset());
        setParent(ui.getParent());
    }
}