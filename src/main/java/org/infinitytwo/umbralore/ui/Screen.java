package org.infinitytwo.umbralore.ui;

import org.infinitytwo.umbralore.event.SubscribeEvent;
import org.infinitytwo.umbralore.event.bus.EventBus;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.Window;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.infinitytwo.umbralore.AdvancedMath.*;

public class Screen {
    private final UIBatchRenderer uiBatchRenderer;
    private final ArrayList<UI> uis;
    private final Window window;
    private long lastFrameTime = System.nanoTime(); // nanoseconds
    private float delta; // in seconds
    private final ConcurrentLinkedQueue<Runnable> runs = new ConcurrentLinkedQueue<>();

    public Screen(UIBatchRenderer uiBatchRenderer, Window window) {
        this.uiBatchRenderer = uiBatchRenderer;
        this.window = window;
        this.uis = new ArrayList<>();

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

        // --- Improved hover logic ---
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

        uiBatchRenderer.begin();
        for (UI ui : uis) {
            if (ui instanceof UpdatableUI updatable) {
                updatable.update(delta);
            }
            ui.draw();
        }
        uiBatchRenderer.flush();

        // --- Run deferred runnables ---
        Runnable r;
        while ((r = runs.poll()) != null) {
            r.run();
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
}
