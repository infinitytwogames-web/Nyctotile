package org.infinitytwo.umbralore.core.ui.display;

import org.infinitytwo.umbralore.core.Mouse;
import org.infinitytwo.umbralore.core.data.TextComponent;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.renderer.FontRenderer;
import org.infinitytwo.umbralore.core.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.ui.UI;
import org.infinitytwo.umbralore.core.ui.UpdatableUI;
import org.infinitytwo.umbralore.core.ui.builtin.Tooltip;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.infinitytwo.umbralore.core.VectorMath.isPointWithinRectangle;
import static org.infinitytwo.umbralore.core.Display.transformWindowToVirtual;

public class Screen {
    private final UIBatchRenderer uiBatchRenderer;
    protected final ArrayList<UI> uis;
    private final Window window;
    private long lastFrameTime = System.nanoTime(); // nanoseconds
    protected float delta; // in seconds
    private final ConcurrentLinkedQueue<Runnable> runs = new ConcurrentLinkedQueue<>();
    private final FontRenderer renderer;
    private final Tooltip tooltip;
    private boolean showTooltip = false;

    public Screen(UIBatchRenderer uiBatchRenderer, Window window) {
        this.uiBatchRenderer = uiBatchRenderer;
        this.window = window;
        this.uis = new ArrayList<>();
        tooltip = new Tooltip(this);

        renderer = new FontRenderer("src/main/resources/font.ttf",32);

        EventBus.register(this);
    }

    public UIBatchRenderer getUIBatchRenderer() {
        return uiBatchRenderer;
    }

    public int register(UI ui) {
        if (!uis.contains(ui)) {
            int l = uis.size();
            uis.add(ui);
            return l;
        }
        return 0;
    }

    public void draw() {
        long now = System.nanoTime();
        delta = (now - lastFrameTime) / 1_000_000_000.0f; // Convert to seconds
        lastFrameTime = now;

        calculateHover();
        uiBatchRenderer.begin();
        drawUIs();
        Mouse.draw();
        uiBatchRenderer.flush();

        Runnable r;
        while ((r = runs.poll()) != null) {
            r.run();
        }
    }

    protected void calculateHover() {
        Vector2f m = window.getMousePosition();
        Vector2i mousePosition = transformWindowToVirtual(window, new Vector2i((int) m.x, (int) m.y));

        boolean hoverHandled = false;
        for (int i = uis.size() - 1; i >= 0; i--) {
            UI ui = uis.get(i);
            boolean hoveringNow = isPointWithinRectangle(ui.getPosition(), mousePosition, ui.getEnd());

            if (hoveringNow && !hoverHandled) {
                if (!ui.isHovering()) {ui.setHovering(true);
                    ui.onMouseHover(new MouseHoverEvent(mousePosition));
                }
                hoverHandled = true;
            } else {
                if (ui.isHovering()) {ui.setHovering(false);
                    ui.onMouseHoverEnded();

                }
            }
        }
    }

    protected void drawUIs() {
        for (UI ui : uis) {
            if (ui instanceof UpdatableUI updatable) {
                updatable.update(delta);
            }
            ui.draw();
        }
    }

    @SubscribeEvent
    public void onMouseClicked(MouseButtonEvent e) {
        Vector2i mousePosition = transformWindowToVirtual(window, e.x, e.y);
        for (int i = uis.size() - 1; i >= 0; i--) {
            if (isPointWithinRectangle(uis.get(i).getPosition(), mousePosition, uis.get(i).getEnd())) {
                uis.get(i).onMouseClicked(e);
                break;
            }
        }
    }

    public Window getWindow() {
        return window;
    }

    public void run(Runnable runnable) {
        runs.add(runnable);
    }

    public FontRenderer getFontRenderer() {
        return renderer;
    }

    public void showTooltip(TextComponent text) {
        showTooltip = true;
        tooltip.setText(text);
    }

    public void hideTooltip() {
        showTooltip = false;
    }

    public void close() {
        for (UI ui : uis) {
            ui.cleanup();
        }
    }

    public void onOpen() {}
}
