package org.infinitytwo.nyctotile.core.ui;

import org.infinitytwo.nyctotile.core.RGBA;
import org.infinitytwo.nyctotile.core.event.input.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.animations.UpdatableUI;
import org.infinitytwo.nyctotile.core.ui.builder.UIBuilder;

public class Caret extends UpdatableUI {
    private float blinkTimer = 0f;
    private boolean visible = true;
    private boolean active = true;

    public static CaretBuilder builder(UIBatchRenderer renderer) {
        return new CaretBuilder(renderer);
    }

    public Caret(UIBatchRenderer renderer) {
        super(renderer);
        setWidth(10);
        setBackgroundColor(new RGBA(1,1,1,1));
    }

    public void update(float delta) {
        if (!active) {
            return;
        }
        
        blinkTimer += delta;
        if (blinkTimer >= 0.5f) {
            blinkTimer = 0f;
            visible = !visible;
        }
    }

    public void reset() {
        blinkTimer = 0f;
    }

    @Override
    public void draw() {
        if (visible && active) {
            super.draw();
        }
    }

    @Override
    public void onMouseClicked(MouseButtonEvent e) {

    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {

    }

    @Override
    public void onMouseHoverEnded() {

    }

    @Override
    public void cleanup() {

    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void forceDraw() {
        super.draw();
    }
    
    public static class CaretBuilder extends UIBuilder<Caret> {
        public CaretBuilder(UIBatchRenderer renderer) {
            super(new Caret(renderer));
        }

        public CaretBuilder active(boolean active) {
            ui.setActive(active);
            return this;
        }

        @Override
        public CaretBuilder applyDefault() {
            return this;
        }
    }
}

