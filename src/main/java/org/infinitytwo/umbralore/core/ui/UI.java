package org.infinitytwo.umbralore.core.ui;

import org.infinitytwo.umbralore.core.Display;
import org.infinitytwo.umbralore.core.RGBA;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.core.ui.position.Anchor;
import org.infinitytwo.umbralore.core.ui.position.Pivot;
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
            Vector2i parentOrigin = parent.getPosition(); // Get parent's calculated screen coordinates
            xa = parentOrigin.x + (int) (parent.width * anchor.x); // Anchor relative to parent's size
            ya = parentOrigin.y + (int) (parent.height * anchor.y);
            o = offset; // Use only this element's local offset
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

    protected void setOffset(int same) {
        setOffset(same,same);
    }

    public void setOffset(Vector2i offset) {
        setOffset(offset.x,offset.y);
    }

    public void setOffset(int x, int y) {
        offset.set(x,y);
    }

    public void setAnchor(Anchor anchor) {
        setAnchor(anchor.x,anchor.y);
    }

    public void setAnchor(float x, float y) {
        anchor.set(x,y);
    }

    public void setPivot(float x, float y) {
        pivot.set(x,y);
    }

    public void setPivot(Pivot pivot) {
        setPivot(pivot.x,pivot.y);
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
        addOffset(v.x, v.y);
    }

    public void setSize(Vector2i size) {
        setSize(size.x,size.y);
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    public void setSize(int same) {
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

    public void addOffset(int x, int y) {
        offset.add(x,y);
    }

    public void addOffset(int same) {
        addOffset(same,same);
    }
}