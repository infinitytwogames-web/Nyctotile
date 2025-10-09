package org.infinitytwo.umbralore.ui.builtin;

import org.infinitytwo.umbralore.data.Inventory;
import org.infinitytwo.umbralore.event.SubscribeEvent;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.input.MouseHoverEvent;
import org.infinitytwo.umbralore.item.Item;
import org.infinitytwo.umbralore.renderer.FontRenderer;
import org.infinitytwo.umbralore.ui.Grid;
import org.infinitytwo.umbralore.ui.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryViewer extends Grid {
    protected Inventory link;
    protected Screen screen;
    protected FontRenderer fontRenderer;
    protected final List<ItemSlot> slots = new ArrayList<>();

    public InventoryViewer(Screen renderer, FontRenderer fontRenderer, int columns) {
        super(renderer.getUIBatchRenderer());
        screen = renderer;
        this.fontRenderer = fontRenderer;
        this.columns = columns;
    }

    public void linkInventory(@NotNull Inventory inventory) {
        int column = 0;
        int row = 0;
        slots.clear();
        this.link = inventory;
        setSize(inventory.getMaxSlots() / columns, columns);

        inventory.getEventBus().register(this);

        for (int i = 0; i < link.getMaxSlots(); i++) {
            ItemSlot slot = new ItemSlot(screen, fontRenderer);
            slot.setItem(link.get(i));
            put(slot, row, column);
            slots.add(slot);

            column++;
            if (column >= columns) {
                column = 0;
                row++;
            }
        }
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

    @Override
    public void cleanup() {
        if (link != null) link.getEventBus().unregister(this);
    }
}
