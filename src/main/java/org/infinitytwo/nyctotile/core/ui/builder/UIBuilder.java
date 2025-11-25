package org.infinitytwo.nyctotile.core.ui.builder;

import org.infinitytwo.nyctotile.core.IBuilder;
import org.infinitytwo.nyctotile.core.RGBA;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.joml.Vector2i;

public abstract class UIBuilder<T extends UI> implements IBuilder<T> {
    protected T ui;

    public UIBuilder(T element) {
        ui = element;
    }

    public UIBuilder<T> width(int width) {
        ui.setWidth(width);
        return this;
    }

    public UIBuilder<T> height(int height) {
        ui.setHeight(height);
        return this;
    }

    public UIBuilder<T> backgroundColor(RGBA color) {
        ui.setBackgroundColor(color);
        return this;
    }

    public UIBuilder<T> backgroundColor(float r, float g, float b, float a) {
        ui.setBackgroundColor(r,g,b,a);
        return this;
    }

    public UIBuilder<T> position(Anchor anchor, Pivot pivot, Vector2i offset) {
        ui.setPosition(anchor,pivot,offset);
        return this;
    }

    public UIBuilder<T> position(Anchor anchor, Pivot pivot) {
        ui.setPosition(anchor,pivot);
        return this;
    }

    public UIBuilder<T> parent(UI parent) {
        ui.setParent(parent);
        return this;
    }

    public abstract UIBuilder<T> applyDefault();

    @Override
    public T build() {
        return ui;
    }
}
