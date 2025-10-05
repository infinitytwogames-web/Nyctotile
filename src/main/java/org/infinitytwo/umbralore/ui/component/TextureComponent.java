package org.infinitytwo.umbralore.ui.component;

import org.infinitytwo.umbralore.RGBA;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.model.TextureAtlas;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.UI;
import org.infinitytwo.umbralore.ui.builder.UIBuilder;
import org.joml.Vector2i;

public class TextureComponent extends UI implements Component {
    protected int textureIndex;
    protected TextureAtlas atlas;
    protected RGBA foregroundColor = new RGBA();

    public TextureComponent(int textureIndex, TextureAtlas atlas, UIBatchRenderer renderer) {
        super(renderer);
        this.textureIndex = textureIndex;
        this.atlas = atlas;
    }

    public RGBA getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(RGBA foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public int getTextureIndex() {
        return textureIndex;
    }

    public void setTextureIndex(int textureIndex) {
        this.textureIndex = textureIndex;
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public void setAtlas(TextureAtlas atlas) {
        this.atlas = atlas;
    }

    @Override
    public void draw() {
        super.draw();

        if (atlas != null && textureIndex >= 0) {
            Vector2i v = getPosition();
            renderer.queueTextured(v.x, v.y, getWidth(), getHeight(),
                    getTextureIndex(), atlas, foregroundColor);
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

    public static class Builder extends UIBuilder<TextureComponent> {
        public Builder(UIBatchRenderer renderer, TextureAtlas atlas, int index) {
            super(renderer, new TextureComponent(index,atlas,renderer));
        }

        @Override
        public UIBuilder<TextureComponent> applyDefault() {
            return null;
        }
    }
}
