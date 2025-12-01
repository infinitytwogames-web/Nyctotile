package org.infinitytwo.nyctotile.core.ui.layout.scroll;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseScrollEvent;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.layout.Scene;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.jetbrains.annotations.NotNull;
import org.infinitytwo.nyctotile.core.Game;

import java.util.ArrayList;

public class ScrollableMenu extends UI {
    protected ArrayList<UI> uis = new ArrayList<>();
    protected int padding = 5;
    protected int contentHeight;
    protected ScrollButton scrollButton;
    protected Scene scene;
    protected int scrollY;
    
    public ScrollableMenu(@NotNull Scene scene, Window window) {
        super(scene.getUIBatchRenderer());
        this.scene = scene;
        
        scrollButton = new ScrollButton(scene.getUIBatchRenderer(),this,window);
        scrollButton.setParent(this);
        scrollButton.setDrawOrder(drawOrder+2);
        scrollButton.setPosition(new Anchor(1,0),new Pivot(1,0));
        
        scene.register(scrollButton);
        EventBus.connect(this);
        
        setupScrollbar();
    }
    
    public void addUI(UI ui) {
        uis.add(ui);
        ui.setParent(this);
        
        contentHeight = Math.max(ui.getHeight()+ui.getPosition().y()+padding,contentHeight);
        ui.setDrawOrder(drawOrder+ui.getDrawOrder()+1);
        
        setupScrollbar();
    }
    
    public void removeUI(UI ui) {
        uis.remove(ui);
        
        contentHeight = 0;
        for (UI u : uis) {
            contentHeight = Math.max(u.getHeight() + u.getPosition().y() + padding, contentHeight);
        }
        
        setupScrollbar();
    }
    
    @Override
    public void draw() {
        super.draw();
        renderer.enableScissor(getPosition(), width, height);
        
        for (UI ui : uis) {
            ui.addOffset(0, scrollY);
            ui.draw();
            ui.addOffset(0, -scrollY);
        }
        
        renderer.disableScissor();
    }
    
    @Override
    public void setDrawOrder(int drawOrder) {
        super.setDrawOrder(drawOrder);
        
        for (UI ui : uis) ui.setDrawOrder(drawOrder+1);
    }
    
    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        scrollButton.setHeight(height);
        
        setupScrollbar();
    }
    
    public int getPadding() {
        return padding;
    }
    
    public void setPadding(int padding) {
        this.padding = padding;
    }
    
    public ScrollButton getScrollButton() {
        return scrollButton;
    }
    
    public void setScrollButton(ScrollButton scrollButton) {
        this.scrollButton = scrollButton;
    }
    
    public int getContentHeight() {
        return contentHeight;
    }
    
    public int getScrollY() {
        return scrollY;
    }
    
    public void setScrollY(int scrollY) {
        int maxScrollDistance = Math.max(0, contentHeight - height);
        this.scrollY = Math.min(maxScrollDistance, scrollY);
        
        if (scrollButton != null) {
            scrollButton.updateHandlePosition(-this.scrollY);
        }
    }
    
    private void setupScrollbar() {
        // 1. Determine total track height (which is the menu's height)
        int trackHeight = this.height;
        
        // 2. Calculate the total scrollable distance
        int maxScrollDistance = contentHeight - trackHeight;
        
        // 3. Handle visibility: Hide the scrollbar if the content fits entirely.
        if (maxScrollDistance <= 0) {
            scrollButton.setHidden(true);
            this.scrollY = 0; // Ensure scroll is reset if content shrinks
            return;
        }
        
        scrollButton.setHidden(false);
        
        // 4. Calculate the proportional handle height (clamped to prevent huge handles)
        float heightRatio = (float) trackHeight / contentHeight;
        int handleHeight = (int) (trackHeight * heightRatio);
        
        // Minimum handle height to ensure it's always draggable (e.g., 20 virtual units)
        int minHandleHeight = 20;
        
        int finalHandleHeight = Math.max(minHandleHeight, handleHeight);
        
        // 5. Apply the calculated height to the draggable button
        scrollButton.setScrollHeight(finalHandleHeight);
        
        // Also update the position in case content height changed
        scrollButton.updateHandlePosition(this.scrollY);
    }
    
    @SubscribeEvent
    public void onMouseScroll(MouseScrollEvent e) {
        if (!hovering) return;
        
        int rawNewScrollY = this.scrollY - (int) (e.y * Game.getSensitivity());
        this.setScrollY(rawNewScrollY);
    }
    
    @Override
    public void onMouseClicked(MouseButtonEvent e) {
    
    }
    
    @Override
    public void onMouseHover(MouseHoverEvent e) {
    
    }
    
    @Override
    public void onMouseHoverEnded() {
    
    }
    
    @Override
    public void cleanup() {
        for (UI ui : uis) ui.cleanup();
    }
}
