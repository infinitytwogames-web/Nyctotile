package org.infinitytwo.nyctotile.core.ui.layout.scroll;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseCoordinatesEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.renderer.UIBatchRenderer;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.builtin.Rectangle;

import static org.infinitytwo.nyctotile.core.manager.Display.transformWindowToVirtual;
import static org.joml.Math.clamp;
import static org.lwjgl.glfw.GLFW.*;

/**
 * This class represents the SCROLL HANDLE (Thumb) that the user drags.
 * It contains a visual representation of the SCROLL TRACK (the parent).
 */
public class ScrollButton extends UI { // The Scroll Handle/Thumb
    protected final ScrollableMenu menu;
    protected final UI scrollTrack; // The visual Scroll Track (Container)
    protected final RGBA original = backgroundColor.copy();
    protected final Window window;
    
    protected volatile boolean hold = false;
    protected volatile int mouseYOffset = 0;
    
    public ScrollButton(UIBatchRenderer renderer, ScrollableMenu menu, Window window) {
        super(renderer);
        this.menu = menu;
        this.window = window;
        
        // The track is created visually separate
        scrollTrack = new Rectangle(renderer);
        scrollTrack.setBackgroundColor(backgroundColor.r() - 0.5f, backgroundColor.g() - 0.5f, backgroundColor.b() - 0.5f, backgroundColor.a());
        super.setParent(scrollTrack);
        
        EventBus.connect(this);
    }
    
    // setScrollHeight sets the height of the DRAGGABLE HANDLE (The Thumb)
    public void setScrollHeight(int height) {
        super.setHeight(height);
    }
    
    @SubscribeEvent
    public void onMouseMovement(MouseCoordinatesEvent e) {
        if (!hold) return;
        
        // 1. Convert current mouse Y to virtual space
        int currentMouseY = transformWindowToVirtual(window, (int) e.getY());
        
        // 2. Calculate the desired new Y position relative to the Scroll Track's top edge.
        int parentScreenY = parent != null? parent.getPosition().y : 0;
        int relativeY = currentMouseY - mouseYOffset - parentScreenY;
        
        // 3. Clamp the button's new Y offset to the track boundaries.
        int maxButtonY = scrollTrack.getHeight() - this.height;
        int clampedYOffset = clamp(0, maxButtonY, relativeY);
        
        // 4. Update the handle's visual position.
        super.setOffset(super.getOffset().x, clampedYOffset);
        
        // 5. Map the button's position to the menu's scrollable content range.
        float scrollPercent = (maxButtonY > 0)? (float) clampedYOffset / maxButtonY : 0.0f;
        
        // Max scroll distance (always positive)
        float maxScrollDistance = Math.max(0.0f, menu.getContentHeight() - menu.getHeight());
        
        // Calculate the distance traveled. Since Down=+Y, the scroll offset is POSITIVE.
        float desiredMenuScroll = scrollPercent * maxScrollDistance;
        
        // Use setScrollY to apply the change, which will also clamp the value
        menu.setScrollY((int) -desiredMenuScroll);
    }
    
    @SubscribeEvent
    public void onMouseClick(MouseButtonEvent e) {
        if (hold && e.action == GLFW_RELEASE) {
            hold = false;
            onMouseHoverEnded();
        }
    }
    
    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.scrollTrack.setWidth(width);
    }
    
    @Override
    public void setDrawOrder(int drawOrder) {
        super.setDrawOrder(drawOrder);
        scrollTrack.setDrawOrder(drawOrder - 1);
    }
    
    @Override
    public void setHeight(int height) {
        scrollTrack.setHeight(height);
    }
    
    @Override
    public void setBackgroundColor(float r, float g, float b, float a) {
        // This is the Handle's color
        super.setBackgroundColor(r, g, b, a);
        original.set(r, g, b, a);
        this.scrollTrack.setBackgroundColor(r - 0.5f, g - 0.5f, b - 0.5f, a);
    }
    
    @Override
    public void onMouseClicked(MouseButtonEvent e) {
        int virtualY = transformWindowToVirtual(window, e.y);
        
        if (e.action == GLFW_PRESS && e.button == GLFW_MOUSE_BUTTON_1) {
            hold = true;
            mouseYOffset = virtualY - getPosition().y;
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
    public void draw() {
        scrollTrack.draw();
        super.draw();
    }
    
    @Override
    public void setAnchor(float x, float y) {
        scrollTrack.setAnchor(x,y);
    }
    
    @Override
    public void setPivot(float x, float y) {
        scrollTrack.setPivot(x,y);
    }
    
    @Override
    public void setOffset(int x, int y) {
        scrollTrack.setOffset(x,y);
    }
    
    @Override
    public void addOffset(int x, int y) {
        scrollTrack.addOffset(x,y);
    }
    
    @Override
    public void onMouseHoverEnded() {
        if (hold) return;
        super.setBackgroundColor(original.r(), original.g(), original.b(), original.a());
    }
    
    @Override
    public void cleanup() {
        // No additional cleanup needed
    }
    
    public void updateHandlePosition(int menuScrollY) {
        // 1. Calculate the total scrollable distance (Max Scroll is positive distance)
        float maxScrollDistance = Math.max(0.0f, menu.getContentHeight() - menu.getHeight());
        
        // Prevent division by zero if content fits entirely
        if (maxScrollDistance <= 0.0f) {
            super.setOffset(super.getOffset().x, 0); // Handle is at the top
            return;
        }
        
        // 2. Calculate the scroll percentage from the menu's scroll offset.
        // Since Down=+Y, menuScrollY is POSITIVE and can be used directly.
        float scrollPercent = (float) menuScrollY / maxScrollDistance; // <-- FIXED: Removed Math.abs
        
        // 3. Calculate the maximum track distance the handle can travel.
        int maxButtonY = scrollTrack.getHeight() - this.height;
        
        // 4. Map the scroll percentage to the handle's visual position.
        int handleYOffset = (int) (scrollPercent * maxButtonY);
        
        // 5. Update the handle's visual position.
        super.setOffset(super.getOffset().x, handleYOffset);
    }
    
    @Override
    public void setParent(UI parent) {
        scrollTrack.setParent(parent);
    }
    
    @Override
    public UI getParent() {
        return scrollTrack.getParent();
    }
    
    @Override
    public void setHidden(boolean hidden) {
        super.setHidden(hidden);
        scrollTrack.setHidden(hidden);
    }
}