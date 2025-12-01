package org.infinitytwo.nyctotile.core.ui.layout;

import org.infinitytwo.nyctotile.core.manager.Display;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.state.WindowResizedEvent;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.joml.Vector2i;

/**
 * A UI element that maintains a fixed aspect ratio derived from initial
 * default width and height, and centers itself within the available display space.
 */
public abstract class AspectRatioPanel extends UI {

    private float defaultWidth;
    private float defaultHeight;
    private float ratio; // Calculated as defaultWidth / defaultHeight

    /**
     * Constructs an AspectRatioPanel using the provided dimensions to establish the aspect ratio.
     * @param renderer The batch renderer.
     */
    public AspectRatioPanel(UIBatchRenderer renderer) {
        super(renderer);

        // Ensure we don't divide by zero or use non-positive values.
        // We set the position anchor and pivot once to always be centered.
        this.anchor = new Anchor(0.5f, 0.5f);
        this.pivot = new Pivot(0.5f, 0.5f);

        EventBus.connect(this);
        // Call the resize logic immediately to set initial size
        updateDimensions();
    }

    // --- Core Aspect Ratio Logic ---

    /**
     * Calculates and applies the dimensions to maintain the fixed aspect ratio.
     * This logic determines whether the window's width or height is the limiting factor.
     */
    private void updateDimensions() {
        if (this.ratio <= 0) {
            // Safety check: if the ratio is invalid, do nothing.
            return;
        }

        // Get the available space (the Display dimensions)
        final float availableWidth = Display.width;
        final float availableHeight = Display.height;

        // 1. Calculate the height required if we use the full available width.
        // height = width / ratio (ratio = W/H)
        float heightByWidth = availableWidth / this.ratio;

        if (heightByWidth <= availableHeight) {
            // Case 1 (Width-Limited): The calculated height fits. Use full available width.
            this.width = (int) availableWidth;
            this.height = (int) heightByWidth;

            // Calculate vertical offset to center (creating letterboxing).
            this.offset = new Vector2i(0, (int) ((availableHeight - this.height) / 2));
        } else {
            // Case 2 (Height-Limited): The calculated height is too tall. Use full available height.
            // width = height * ratio (ratio = W/H)
            float widthByHeight = availableHeight * this.ratio;

            this.width = (int) widthByHeight;
            this.height = (int) availableHeight;

            // Calculate horizontal offset to center (creating pillarboxing).
            this.offset = new Vector2i((int) ((availableWidth - this.width) / 2), 0);
        }
    }

    // --- Event Handling ---

    @SubscribeEvent
    public void onWindowResize(WindowResizedEvent e) {
        // Recalculate dimensions every time the window size changes
        updateDimensions();
    }

    @Override
    public void cleanup() {
//        EventBus.unregister(this);
    }

    @Override
    public void setWidth(int width) {
        defaultWidth = width;
    }

    @Override
    public void setHeight(int height) {
        defaultHeight = height;
    }
}
