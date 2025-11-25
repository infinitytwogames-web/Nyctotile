package org.infinitytwo.nyctotile.core.ui.display;

import org.infinitytwo.nyctotile.core.manager.Mouse;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.animations.UpdatableUI;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.infinitytwo.nyctotile.core.VectorMath.isPointWithinRectangle;
import static org.infinitytwo.nyctotile.core.Display.transformWindowToVirtual;

public class Scene {
    protected final UIBatchRenderer renderer;
    protected final ArrayList<UI> uis;
    protected final Window window;
    private long lastFrameTime = System.nanoTime(); // nanoseconds
    protected float delta; // in seconds
    private final ConcurrentLinkedQueue<Runnable> runs = new ConcurrentLinkedQueue<>();
    protected boolean handleInput = true;
    
    public Scene(UIBatchRenderer renderer, Window window) {
        this.renderer = renderer;
        this.window = window;
        this.uis = new ArrayList<>();
        
        EventBus.connect(this);
    }
    
    public UIBatchRenderer getUIBatchRenderer() {
        return renderer;
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
        
        Collections.sort(uis);
        
        calculateHover();
        renderer.begin();
        drawUIs();
        Mouse.draw();
        renderer.flush();
        
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
            
            if (ui.isHidden()) {
                if (ui.isHovering()) {
                    ui.setHovering(false);
                    ui.onMouseHoverEnded();
                }
                continue;
            }
            
            boolean hoveringNow = isPointWithinRectangle(ui.getPosition(), mousePosition, ui.getEndPoint());
            
            if (hoveringNow && !hoverHandled) {
                if (!ui.isHovering()) {
                    ui.setHovering(true);
                    ui.onMouseHover(new MouseHoverEvent(mousePosition));
                }
                hoverHandled = true;
            } else {
                if (ui.isHovering()) {
                    ui.setHovering(false);
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
        if (!handleInput) return;
        Vector2i mousePosition = transformWindowToVirtual(window, e.x, e.y);
        
        // Iterate backward (front-most to back-most)
        for (int i = uis.size() - 1; i >= 0; i--) {
            UI ui = uis.get(i);
            
            // If hidden, skip this element for interaction.
            if (ui.isHidden()) {
                continue;
            }
            
            // Check for intersection
            if (isPointWithinRectangle(ui.getPosition(), mousePosition, ui.getEndPoint())) {
                ui.onMouseClicked(e);
                break; // Stop after clicking the front-most, non-hidden element.
            }
        }
    }
    
    public Window getWindow() {
        return window;
    }
    
    public void run(Runnable runnable) {
        runs.add(runnable);
    }
    
    public void close() {
        for (UI ui : uis) {
            ui.cleanup();
        }
        setHandleInput(false);
    }
    
    public void open() {
        setHandleInput(true);
    }
    
    public void setHandleInput(boolean handleInput) {
        this.handleInput = handleInput;
    }
    
    public void stop() {
        setHandleInput(false);
    }
}
