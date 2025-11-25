package org.infinitytwo.nyctotile.core.ui.animations;

import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.UI;

public abstract class UpdatableUI extends UI {
    public UpdatableUI(UIBatchRenderer renderer) {
        super(renderer);
    }

    public abstract void update(float delta);
}
