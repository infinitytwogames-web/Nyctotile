package org.infinitytwo.umbralore.core.ui.input;

import org.infinitytwo.umbralore.core.RGBA;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.ui.Label;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.position.Anchor;
import org.infinitytwo.umbralore.core.ui.position.Pivot;
import org.joml.Vector2i;

import java.nio.file.Path;

public abstract class Button extends Label {
    protected RGBA original;

    public Button(Screen renderer, Path path, String s) {
        super(renderer, path);
        original = getBackgroundColor();

        setText(s);
        setTextPosition(new Anchor(0.5f,0.5f),new Pivot(0.5f,0.5f),new Vector2i());
    }

    @Override
    public void setBackgroundColor(RGBA backgroundColor) {
        original = backgroundColor;
        super.setBackgroundColor(backgroundColor);
    }

    @Override
    public void setBackgroundColor(float r, float g, float b, float a) {
        original.set(r, g, b, a);
        super.setBackgroundColor(r,g,b,a);
    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {
        float d = 0.25f;
        backgroundColor.set(
                Math.max(0, original.r() - d),
                Math.max(0, original.g() - d),
                Math.max(0, original.b() - d),
                original.a()
        );
    }


    @Override
    public void onMouseHoverEnded() {
        backgroundColor.set(original);
    }
    
    @Override
    public abstract void onMouseClicked(MouseButtonEvent e);
}