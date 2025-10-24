package org.infinitytwo.umbralore.core.ui.builtin;

import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.Inventory;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.core.data.Item;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.renderer.FontRenderer;
import org.infinitytwo.umbralore.core.renderer.UIBatchRenderer;
import org.infinitytwo.umbralore.core.ui.display.Grid;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryViewer extends Grid {
    protected Inventory link;
    protected Screen screen;
    protected FontRenderer fontRenderer;
    protected final List<ItemSlot> slots = new ArrayList<>();
    protected final Window window;
    private TextureAtlas atlas;
    protected Factory factory;

    public InventoryViewer(Screen screen, FontRenderer fontRenderer, Window window, int columns) {
        super(screen.getUIBatchRenderer());
        this.screen = screen;
        this.fontRenderer = fontRenderer;
        this.columns = columns;
        this.window = window;
    }

    public InventoryViewer(Screen screen, FontRenderer fontRenderer, Window window, Factory factory, int columns) {
        super(screen.getUIBatchRenderer());
        this.screen = screen;
        this.fontRenderer = fontRenderer;
        this.window = window;
        this.factory = factory;
        this.columns = columns;
    }

    public void linkInventory(@NotNull Inventory inventory) {
        int column = 0;
        int row = 0;
        slots.clear();
        this.link = inventory;
        this.rows = (int) Math.ceil((double) inventory.getMaxSlots() / columns); // Inherited from Grid

        updateSize();

        inventory.getEventBus().register(this);

        for (int i = 0; i < link.getMaxSlots(); i++) {
            ItemSlot slot = getItemSlot(i);
            put(slot, row, column);
            slots.add(slot);

            column++;
            if (column >= columns) {
                column = 0;
                row++;
            }
        }
    }

    @NotNull
    public ItemSlot getItemSlot(int i) {
        ItemSlot result;
        if (factory == null) {
            ItemSlot slot = new ItemSlot(screen, fontRenderer, window);
            slot.setAtlas(atlas);
            slot.setItem(link.get(i));
            result = slot;
        } else {
            result = factory.create(i, link.get(i), screen, fontRenderer, window);
        }
        return result;
    }

    @SubscribeEvent
    public void refresh(Inventory.ChangedEvent e) {
        if (e == null) {
            // refresh all
            for (int i = 0; i < link.getMaxSlots(); i++) {
                slots.get(i).setItem(link.get(i));
            }
        } else {
            if (link == null || e.slot >= slots.size()) return;
            slots.get(e.slot).setItem(link.get(e.slot));
        }
    }

    public void refresh() { refresh(null); }

    public void set(int slot, Item item) {
        link.set(slot, item);
    }

    public void setCount(int slot, int count) {
        link.setCount(slot, count);
    }

    public void remove(int slot) {
        link.remove(slot);
    }

    public int getCount(int slot) {
        return link.getCount(slot);
    }

    public void add(int slot, int addition) {
        link.add(slot, addition);
    }

    public boolean isEmpty() {
        return link.isEmpty();
    }

    public boolean isFull() {
        return link.isFull();
    }

    public void clear() {
        link.clear();
    }

    public int getMaxSlots() {
        return link.getMaxSlots();
    }

    public Map<Integer, Item> getItems() {
        return link.getItems();
    }

    public int add(Item item) {
        return link.add(item);
    }

    public Item getItem(int slot) {
        return link.get(slot);
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

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public void setAtlas(TextureAtlas atlas) {
        this.atlas = atlas;
    }

    @Override
    public void cleanup() {
        if (link != null) link.getEventBus().unregister(this);
    }

    public interface Factory {
        ItemSlot create(int slot, Item item, Screen screen, FontRenderer fontRenderer, Window window);
    }
}
