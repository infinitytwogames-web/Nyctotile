package org.infinitytwo.umbralore.ui;

import org.infinitytwo.umbralore.RGBA;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.builder.UIBuilder;

public class Caret extends UpdatableUI {
    private float blinkTimer = 0f;
    private boolean visible = true;
    private boolean active = true;

    public static CaretBuilder builder(UIBatchRenderer renderer, int height) {
        return new CaretBuilder(renderer,height);
    }

    public Caret(UIBatchRenderer renderer, int height) {
        super(renderer);
        setWidth(5);
        setHeight(height);
        setBackgroundColor(new RGBA(1,1,1,1));
    }

    public void update(float delta) {
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

    public static class CaretBuilder extends UIBuilder<Caret> {
        public CaretBuilder(UIBatchRenderer renderer, int height) {
            super(renderer, new Caret(renderer, height));
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

