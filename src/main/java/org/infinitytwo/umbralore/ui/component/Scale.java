package org.infinitytwo.umbralore.ui.component;

import org.infinitytwo.umbralore.Display;
import org.infinitytwo.umbralore.event.SubscribeEvent;
import org.infinitytwo.umbralore.event.bus.EventBus;
import org.infinitytwo.umbralore.event.state.WindowResizedEvent;

public class Scale implements Component {
    protected float xRatio, yRatio;
    protected int width, height;

    public Scale(float xRatio, float yRatio) {
        this.xRatio = xRatio;
        this.yRatio = yRatio;

        EventBus.register(this);
    }

    @SubscribeEvent
    public void windowResize(WindowResizedEvent e) {
        width = (int) (Display.width * xRatio);
        height = (int) (Display.height * yRatio);
    }

    @Override
    public void draw() {

    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
