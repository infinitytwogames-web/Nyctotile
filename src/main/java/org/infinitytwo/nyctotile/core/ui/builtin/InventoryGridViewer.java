package org.infinitytwo.nyctotile.core.ui.builtin;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.Inventory;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.input.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.data.Item;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.renderer.FontRenderer;
import org.infinitytwo.nyctotile.core.ui.display.Grid;
import org.infinitytwo.nyctotile.core.ui.display.Scene;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InventoryGridViewer extends Grid {
    protected Inventory link;
    protected Scene scene;
    protected FontRenderer fontRenderer;
    protected final List<ItemSlot> slots = new ArrayList<>();
    protected final Window window;
    private TextureAtlas atlas;
    protected Factory factory;

    public InventoryGridViewer(Scene scene, FontRenderer fontRenderer, Window window, int columns) {
        super(scene);
        this.scene = scene;
        this.fontRenderer = fontRenderer;
        this.columns = columns;
        this.window = window;
    }

    public InventoryGridViewer(Scene scene, FontRenderer fontRenderer, Window window, Factory factory, int columns) {
        super(scene);
        this.scene = scene;
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

        inventory.getEventBus().register(this);
    }

    @NotNull
    public ItemSlot getItemSlot(int i) {
        ItemSlot result;
        if (factory == null) {
            ItemSlot slot = new ItemSlot(scene, fontRenderer, window);
            slot.setAtlas(atlas);
            slot.setItem(link.get(i));
            result = slot;
        } else {
            result = factory.create(i, link.get(i), scene, fontRenderer, window);
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
        if (link == null) return;
        link.set(slot, item);
    }

    public void setCount(int slot, int count) {
        if (link == null) return;
        link.setCount(slot, count);
    }

    public void remove(int slot) {
        if (link == null) return;
        link.remove(slot);
    }

    public int getCount(int slot) {
        if (link == null) return 0;
        return link.getCount(slot);
    }

    public void add(int slot, int addition) {
        if (link == null) return;
        link.add(slot, addition);
    }

    public boolean isEmpty() {
        if (link == null) return true;
        return link.isEmpty();
    }

    public boolean isFull() {
        if (link == null) return false;
        return link.isFull();
    }

    public void clear() {
        if (link == null) return;
        link.clear();
    }

    public int getMaxSlots() {
        if (link == null) return 0;
        return link.getMaxSlots();
    }

    public Collection<Item> getItems() {
        if (link == null) return null;
        return link.getItems();
    }

    public int addItem(Item item) {
        if (link == null) return 0;
        return link.add(item);
    }

    public Item getItem(int slot) {
        if (link == null) return null;
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
        ItemSlot create(int slot, Item item, Scene scene, FontRenderer fontRenderer, Window window);
    }
}
