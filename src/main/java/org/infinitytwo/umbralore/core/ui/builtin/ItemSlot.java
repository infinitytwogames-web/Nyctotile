package org.infinitytwo.umbralore.core.ui.builtin;

import org.infinitytwo.umbralore.core.Mouse;
import org.infinitytwo.umbralore.core.RGBA;
import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.data.Item;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.registry.ItemRegistry;
import org.infinitytwo.umbralore.core.registry.ResourceManager;
import org.infinitytwo.umbralore.core.renderer.FontRenderer;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.UI;
import org.infinitytwo.umbralore.core.ui.component.ItemHolder;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class ItemSlot extends UI {
    protected final ItemHolder item;

    public ItemSlot(Screen screen, FontRenderer fontRenderer, Window window) {
        super(screen.getUIBatchRenderer());
        item = new ItemHolder(ResourceManager.items, screen, 0, fontRenderer);
        screen.register(this);
        item.setParent(this);
    }

    public Item getItem() {
        return item.getItem();
    }

    public void setItem(Item item) {
        if (item == null) {
            this.item.setItem(null);
            this.item.setTextureIndex(-1);
        } else {
            this.item.setItem(item);
            int textureIndex = ItemRegistry.getMainRegistry().getTextureIndex(item.getType().getIndex());
            this.item.setTextureIndex(textureIndex);
        }
    }

    public Screen getScreen() {
        return item.getScreen();
    }

    public RGBA getForegroundColor() {
        return item.getForegroundColor();
    }

    public void setForegroundColor(RGBA foregroundColor) {
        item.setForegroundColor(foregroundColor);
    }

    public int getTextureIndex() {
        return item.getTextureIndex();
    }

    public void setTextureIndex(int textureIndex) {
        item.setTextureIndex(textureIndex);
    }

    public TextureAtlas getAtlas() {
        return item.getAtlas();
    }

    public void setAtlas(TextureAtlas atlas) {
        item.setAtlas(atlas);
    }

    @Override
    public void draw() {
        item.setSize(width, height);
        item.setAnchor(0, 0);
        item.setOffset(0, 0);
        item.setPivot(0, 0);
        super.draw();
        item.draw();
    }

    @Override
    public void onMouseClicked(MouseButtonEvent e) {
        if (e.action == GLFW_PRESS) {
            if (Mouse.getItem() != null && getItem() != null &&
                    getItem().getType() == Mouse.getItem().getType()) {
                item.setCount(item.getCount() + Mouse.getItem().getCount());
                Mouse.setItem(null);
            } else {
                Item i = getItem();
                Mouse.renderUsing(getAtlas(), getTextureIndex());
                setItem(Mouse.getItem());
                Mouse.setItem(i);
            }
        }
    }

    @Override
    public void onMouseHover(MouseHoverEvent e) {

    }

    @Override
    public void onMouseHoverEnded() {

    }

    @Override
    public void cleanup() {

    }
}
