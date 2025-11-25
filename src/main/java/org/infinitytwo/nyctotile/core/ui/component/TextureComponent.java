package org.infinitytwo.nyctotile.core.ui.component;

import org.infinitytwo.nyctotile.core.RGBA;
import org.infinitytwo.nyctotile.core.event.input.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.builder.UIBuilder;

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
            renderer.queueTextured(
                    getTextureIndex(), atlas, foregroundColor, this);
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
            super(new TextureComponent(index, atlas, renderer));
        }
        
        @Override
        public UIBuilder<TextureComponent> applyDefault() {
            return this;
        }
    }
}
