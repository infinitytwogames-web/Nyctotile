package org.infinitytwo.nyctotile.core.ui.builtin;

import org.infinitytwo.nyctotile.core.data.Inventory;
import org.infinitytwo.nyctotile.core.data.Item;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseHoverEvent;
import org.infinitytwo.nyctotile.core.ui.UI;
import org.infinitytwo.nyctotile.core.ui.layout.Scene;

import java.util.Collection;

public class InventoryViewer extends UI {
    private final Scene scene;

    protected ItemSlot[] slots;
    protected Inventory inventory;

    public InventoryViewer(Scene renderer) {
        super(renderer.getUIBatchRenderer());
        this.scene = renderer;
    }

    public void linkInventory(Inventory inventory) {
        this.inventory = inventory;
        inventory.getEventBus().register(this);

        slots = new ItemSlot[inventory.getMaxSlots()];
    }

    public void put(ItemSlot slot, int id) {
        if (id > slots.length) throw new IndexOutOfBoundsException("The slot id is out of bounds.");
        slots[id] = slot;
        scene.register(slot);
    }

    @SubscribeEvent
    public void onChanged(Inventory.ChangedEvent e) {
        if (e == null) {
            for (int i = 0; i < slots.length; i++) {
                slots[i].setItem(inventory.get(i));
            }
        } else slots[e.slot].setItem(inventory.get(e.slot));
    }

    public void refresh() { onChanged(null); }

    public void setItem(int slot, Item item) {
        inventory.set(slot, item);
    }

    public Item getItem(int slot) {
        return inventory.get(slot);
    }

    public void remove(int slot) {
        inventory.remove(slot);
    }

    public void setCount(int slot, int count) {
        inventory.setCount(slot, count);
    }

    public int addItem(Item item) {
        return inventory.add(item);
    }

    public int getCount(int slot) {
        return inventory.getCount(slot);
    }

    public void addCount(int slot, int addition) {
        inventory.add(slot, addition);
    }

    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    public EventBus getEventBus() {
        return inventory.getEventBus();
    }

    public Collection<Item> getItems() {
        return inventory.getItems();
    }

    public boolean isFull() {
        return inventory.isFull();
    }

    public void clear() {
        inventory.clear();
    }

    public int getMaxSlots() {
        return inventory.getMaxSlots();
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

    }
}
