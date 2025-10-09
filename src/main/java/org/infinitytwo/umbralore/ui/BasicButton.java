package org.infinitytwo.umbralore.ui;

import org.infinitytwo.umbralore.RGBA;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;

public abstract class BasicButton extends UI {
    protected RGBA original = new RGBA(backgroundColor);

    public BasicButton(UIBatchRenderer renderer) {
        super(renderer);
    }

    @Override
    public void onMouseHoverEnded() {
        backgroundColor = original;
    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {
        backgroundColor.set(
                original.r() - 0.5f,
                original.g() - 0.5f,
                original.b() - 0.5f,
                original.a()
        );
    }
}
