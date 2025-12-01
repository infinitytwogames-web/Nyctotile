package org.infinitytwo.nyctotile.core.ui.layout;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.VectorMath;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.UI;

import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public abstract class BasicButton extends UI {
    protected RGBA original = backgroundColor.copy();
    
    public BasicButton(UIBatchRenderer renderer) {
        super(renderer);
    }
    
    @Override
    public void onMouseClicked(MouseButtonEvent e) {
        if (e.action == GLFW_RELEASE) {
            if (VectorMath.isPointWithinRectangle(getPosition(),e.x,e.y, getEndPoint())) {
                clicked(e);
            }
        }
    }
    
    @Override
    public void onMouseHover(MouseHoverEvent e) {
        super.setBackgroundColor(
                original.r() - 0.25f,
                original.g() - 0.25f,
                original.b() - 0.25f,
                original.a()
        );
    }
    
    @Override
    public void onMouseHoverEnded() {
        backgroundColor.set(original);
    }
    
    @Override
    public void cleanup() {
    
    }
    
    public abstract void clicked(MouseButtonEvent e);
}
