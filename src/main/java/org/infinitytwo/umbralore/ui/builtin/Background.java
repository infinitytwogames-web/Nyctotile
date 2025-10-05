package org.infinitytwo.umbralore.ui.builtin;

import org.infinitytwo.umbralore.Display;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.UI;
import org.infinitytwo.umbralore.ui.builder.UIBuilder;
import org.infinitytwo.umbralore.ui.component.Scale;
import org.infinitytwo.umbralore.ui.position.Anchor;
import org.infinitytwo.umbralore.ui.position.Pivot;
import org.joml.Vector2i;

/**
 * A built-in UI component that is designed to always cover the entire window.
 * It automatically subscribes to WindowResizedEvent to maintain full-screen size.
 */
public class Background extends UI {
    public Scale scale = new Scale(1,1);

    public Background(UIBatchRenderer renderer) {
        super(renderer);
    }

    @Override
    public void onMouseClicked(MouseButtonEvent e) {
        // Background usually doesn't handle clicks, unless it's blocking
    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {
        // Background usually doesn't need hover logic
    }

    @Override
    public void onMouseHoverEnded() {
        // Background usually doesn't need hover logic
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void draw() {
        setWidth(scale.getWidth());
        setHeight(scale.getHeight());
        super.draw();
    }

    /**
     * Builder for the Background UI element, enforcing full-screen and centered properties.
     */
    public static class Builder extends UIBuilder<Background> {
        public Builder(UIBatchRenderer renderer) {
            super(renderer, new Background(renderer));
        }

        @Override
        public UIBuilder<Background> width(int width) {
            // Enforce full width and height, ignoring the passed width.
            // Call super to ensure base UIBuilder logic is also executed.
            super.width(Display.width);
            super.height(Display.height);
            return this;
        }

        @Override
        public UIBuilder<Background> height(int height) {
            // Enforce full height and width, ignoring the passed height.
            // Call super to ensure base UIBuilder logic is also executed.
            super.height(Display.height);
            super.width(Display.width);
            return this;
        }

        // --- Position Overrides to Enforce Centering ---

        private static final Anchor CENTER_ANCHOR = new Anchor(0.5f, 0.5f);
        private static final Pivot CENTER_PIVOT = new Pivot(0.5f, 0.5f);

        @Override
        public UIBuilder<Background> position(Anchor anchor, Pivot pivot, Vector2i offset) {
            // Enforce centered Anchor and Pivot, but respect the offset if provided.
            super.position(CENTER_ANCHOR, CENTER_PIVOT, offset);
            return this;
        }

        @Override
        public UIBuilder<Background> position(Anchor anchor, Pivot pivot) {
            // Enforce centered Anchor and Pivot with no offset.
            super.position(CENTER_ANCHOR, CENTER_PIVOT);
            return this;
        }

        @Override
        public UIBuilder<Background> applyDefault() {
            // Apply the standard full-screen, centered default properties
            super.position(CENTER_ANCHOR, CENTER_PIVOT);
            super.width(Display.width);
            super.height(Display.height);
            return this;
        }
    }
}
