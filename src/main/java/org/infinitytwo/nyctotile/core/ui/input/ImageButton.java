package org.infinitytwo.nyctotile.core.ui.input;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.component.TextureComponent;

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
