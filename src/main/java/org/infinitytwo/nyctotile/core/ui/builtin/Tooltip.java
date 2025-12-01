package org.infinitytwo.nyctotile.core.ui.builtin;

import org.infinitytwo.nyctotile.core.constants.Constants;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.event.state.WindowResizedEvent;
import org.infinitytwo.nyctotile.core.renderer.FontRenderer;
import org.infinitytwo.nyctotile.core.ui.layout.Scene;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.component.Scale;
import org.infinitytwo.nyctotile.core.ui.component.Text;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;

public class Tooltip extends UI {
    protected Text text;
    protected Scale scale = new Scale(0.75f,0.15f);
    protected FontRenderer fontRenderer;

    public Tooltip(Scene scene) {
        super(scene.getUIBatchRenderer());

        fontRenderer = new FontRenderer(Constants.fontFilePath,64);

        setBackgroundColor(0,0,0,0.5f);

        setPosition(new Anchor(0.5f,1), new Pivot(0.5f,1));
        text = new Text(fontRenderer, scene);
        text.setPosition(new Anchor(0.5f,0.5f), new Pivot(0.5f,0.5f));
        text.setParent(this);

        scale.windowResize(new WindowResizedEvent(scene.getWindow()));

    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public String getText() {
        return text.getText();
    }

    @Override
    public void onMouseClicked(MouseButtonEvent e) {

    }

    @Override
    public void onMouseHover(MouseHoverEvent e) { // Btw this refers to this Tooltip not other classes

    }

    @Override
    public void onMouseHoverEnded() {

    }

    @Override
    public void cleanup() {
        fontRenderer.cleanup();
    }

    @Override
    public void draw() {
        setSize(scale.getWidth(),scale.getHeight());
        super.draw();
    }
}
