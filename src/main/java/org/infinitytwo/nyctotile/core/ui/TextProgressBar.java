package org.infinitytwo.nyctotile.core.ui;

import org.infinitytwo.nyctotile.core.RGBA;
import org.infinitytwo.nyctotile.core.ui.builder.RectangleBuilder;
import org.infinitytwo.nyctotile.core.ui.builder.UIBuilder;
import org.infinitytwo.nyctotile.core.ui.builtin.Rectangle;
import org.infinitytwo.nyctotile.core.ui.display.Scene;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.joml.Vector2i;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class TextProgressBar extends Label {
    protected AtomicInteger total = new AtomicInteger(100);
    protected AtomicInteger current = new AtomicInteger(0);
    protected Rectangle bar;

    protected int percentage = 0;

    public TextProgressBar(Scene renderer, Path font, int max) {
        super(renderer,font);
        bar = new Rectangle(renderer.getUIBatchRenderer());
        total.set(max);

        setText("0%");
        setTextPosition(new Anchor(0.5f,0.5f), new Pivot(0.5f,0.5f), new Vector2i());

        bar.setBackgroundColor(new RGBA(1,0,1f,1));
        bar.setPosition(new Anchor(0,0.5f), new Pivot(0,0.5f));
        bar.setParent(this);
    }

    public static ProgressBarBuilder builder(Scene renderer, Path font) {
        return new ProgressBarBuilder(renderer,font);
    }

    public int getTotal() {
        return total.get();
    }

    public void setTotal(int total) {
        this.total.set(total);
    }

    public int getCurrent() {
        return current.get();
    }

    public void setCurrent(int current) {
        if (current <= total.get()) this.current.set(current);
        else this.current.set(total.get());
    }

    public void incrementCurrent() {
        if (current.get() +1 > total.get()) return;
        current.incrementAndGet();
    }

    public void update() {
        percentage = (int)(((float) current.get() / total.get()) * 100);
        setText(percentage+"%");
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        bar.setHeight(height);
    }

    @Override
    public void draw() {
        update();
        bar.setWidth((int) (width * (percentage /100f)));
        super.draw();
        bar.draw();
    }

    @Override
    public void cleanup() {

    }

    public static class ProgressBarBuilder extends UIBuilder<TextProgressBar> {
        public ProgressBarBuilder(Scene renderer, Path font) {
            super(new TextProgressBar(renderer, font, 10));
        }

        public ProgressBarBuilder max(int max) {
            ui.setTotal(max);
            return this;
        }

        public ProgressBarBuilder current(int current) {
            ui.setCurrent(current);
            return this;
        }

        public ProgressBarBuilder bar(RectangleBuilder builder) {
            ui.bar = builder.build();
            return this;
        }

        @Override
        public ProgressBarBuilder applyDefault() {
            return this;
        }
    }
}
