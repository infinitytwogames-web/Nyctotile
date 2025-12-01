package org.infinitytwo.nyctotile.core.ui.builder;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.builtin.Rectangle;

public class RectangleBuilder extends UIBuilder<Rectangle> {
    public RectangleBuilder(UIBatchRenderer renderer, Rectangle element) {
        super(element);
    }

    @Override
    public UIBuilder<Rectangle> applyDefault() {
        backgroundColor(new RGBA(0,0,0,1));
        return this;
    }
}
