package org.infinitytwo.nyctotile.core.ui.component;

import org.infinitytwo.nyctotile.core.Display;
import org.infinitytwo.nyctotile.core.RGB;
import org.infinitytwo.nyctotile.core.renderer.FontRenderer;
import org.infinitytwo.nyctotile.core.ui.display.Scene;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

public class Text implements Component {
    protected Anchor anchor = new Anchor();
    protected Pivot pivot = new Pivot(0, 0);
    protected Vector2i offset = new Vector2i();
    protected UI parent = null;
    protected FontRenderer renderer;
    protected String text = "";
    protected RGB color = new RGB(1, 1, 1);
    protected boolean centerY = true;
    protected Scene scene;
    protected float angle;
    private int drawOrder;
    
    public Text(FontRenderer renderer, Scene scene) {
        this.renderer = renderer;
        this.scene = scene;
    }

    public boolean isCenterY() {
        return centerY;
    }

    public void setCenterY(boolean centerY) {
        this.centerY = centerY;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    public Pivot getPivot() {
        return pivot;
    }

    public void setPivot(Pivot pivot) {
        this.pivot = pivot;
    }

    public Vector2i getOffset() {
        return offset;
    }

    public void setOffset(Vector2i offset) {
        this.offset = offset;
    }

    public UI getParent() {
        return parent;
    }

    public void setParent(UI parent) {
        this.parent = parent;
    }

    public void setPosition(Anchor anchor, Pivot pivot, Vector2i offset) {
        setAnchor(anchor);
        setPivot(pivot);
        setOffset(offset);
    }
    
    public Vector2i getPosition() {
        if (renderer == null) return new Vector2i();
        int xa, ya;
        Vector2i o;
        if (parent == null) {
            xa = (int) (Display.width * anchor.x);
            ya = (int) (Display.height * anchor.y);
            o = offset;
        } else {
            xa = (int) (parent.getWidth() * anchor.x);
            ya = (int) (parent.getHeight() * anchor.y);
            int div = centerY ? 2 : 1;
            o = new Vector2i(offset).add(parent.getPosition()).add(0, (int) (renderer.getFontHeight() /div));
        }

        int xp = (int) (renderer.getStringWidth(text) * pivot.x());
        int yp = (int) ((renderer.getFontHeight() / 2) * pivot.y());

        int x = xa + xp;
        int y = ya + yp;
        return new Vector2i(x + o.x, y + o.y);
    }

    public RGB getColor() {
        return color;
    }

    public void setColor(@NotNull RGB color) {
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public void setText(@NotNull String text) {
        this.text = text;
    }

    public void draw() {
        renderer.renderText(text, getPosition(),drawOrder+1, color, angle);
    }

    public void setPosition(Anchor anchor, Pivot pivot) {
        setAnchor(anchor);
        setPivot(pivot);
    }

    public int getTextSize(String string) {
        return (int) renderer.getStringWidth(string);
    }

    public void setOffset(int x, int y) {
        offset.set(x,y);
    }

    public void setRenderer(FontRenderer textRenderer) {
        renderer = textRenderer;
    }

    public FontRenderer getRenderer() {
        return renderer;
    }
    
    public float getAngle() {
        return angle;
    }
    
    @Override
    public void setAngle(float angle) {
        this.angle = angle;
    }
    
    @Override
    public void setDrawOrder(int z) {
        drawOrder = z;
    }
    
    @Override
    public int getDrawOrder() {
        return drawOrder;
    }
}
