package org.infinitytwo.umbralore.ui.input;

import org.infinitytwo.umbralore.RGBA;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.model.TextureAtlas;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.UI;
import org.infinitytwo.umbralore.ui.component.TextureComponent;

public abstract class ImageButton extends UI {
    protected TextureComponent texture;

    public ImageButton(UIBatchRenderer renderer, TextureAtlas atlas, int textureIndex) {
        super(renderer);
        texture = new TextureComponent(textureIndex,atlas,renderer);
    }

    @Override
    public void draw() {
        super.draw();
        texture.set(this);
    }

    public void setTextureAtlas(TextureAtlas atlas) {
        texture.setAtlas(atlas);
    }

    public void setTextureIndex(int index) {
        texture.setTextureIndex(index);
    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {
        texture.setForegroundColor(new RGBA(0,0,0,0.5f));
    }

    @Override
    public void onMouseHoverEnded() {
        texture.setForegroundColor(new RGBA());
    }

    @Override
    public void cleanup() {

    }
}
