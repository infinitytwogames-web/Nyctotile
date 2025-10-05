package org.infinitytwo.umbralore.ui;

import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.model.TextureAtlas;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.ui.input.ImageButton;

public class ItemSlot extends ImageButton {
    public ItemSlot(UIBatchRenderer renderer, TextureAtlas atlas) {
        super(renderer, atlas, -1);
    }

    @Override
    public void onMouseClicked(MouseButtonEvent e) {

    }
}
