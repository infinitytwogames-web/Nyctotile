package org.infinitytwo.nyctotile.core.ui.component;

import org.infinitytwo.nyctotile.core.Display;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.state.WindowResizedEvent;
import org.infinitytwo.nyctotile.core.ui.UI;

public class Scale implements Component {
    protected UI parent;
    protected UI scaleTo;
    protected float xRatio, yRatio;
    protected int width, height;

    public Scale(float xRatio, float yRatio) {
        this.xRatio = xRatio;
        this.yRatio = yRatio;

        updateSize(); // initialize once
        EventBus.connect(this);
    }

    public Scale(float xRatio, float yRatio, UI ui) {
        this.xRatio = xRatio;
        this.yRatio = yRatio;
        this.parent = ui;

        updateSize(); // initialize once
        EventBus.connect(this);
    }
    
    public void setScaleTo(UI scaleTo) {
        this.scaleTo = scaleTo;
    }
    
    private void updateSize() {
        if (parent != null) {
            if (scaleTo == null) {
                width = xRatio < 0? parent.getWidth() : (int) (Display.width * xRatio);
                height = yRatio < 0? parent.getHeight() : (int) (Display.height * yRatio);
                parent.setSize(width, height);
            } else {
                width = xRatio < 0? parent.getWidth() : (int) (Display.width * xRatio);
                height = yRatio < 0? parent.getHeight() : (int) (Display.height * yRatio);
                parent.setSize(width, height);
            }
        } else {
            width = (int) (Display.width * xRatio);
            height = (int) (Display.height * yRatio);
        }
    }

    public void setRatios(float xRatio, float yRatio) {
        this.xRatio = xRatio;
        this.yRatio = yRatio;
        updateSize();
    }

    @SubscribeEvent
    public void windowResize(WindowResizedEvent e) {
        updateSize();
    }


    @Override
    public void draw() {

    }
    
    @Override
    public void setAngle(float angle) {
    
    }
    
    @Override
    public void setDrawOrder(int z) {
    
    }
    
    @Override
    public int getDrawOrder() {
        return 0;
    }
    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
