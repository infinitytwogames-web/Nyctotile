package org.infinitytwo.nyctotile.core.data;

import org.infinitytwo.nyctotile.core.event.Event;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Inventory {
    protected final int maxSlots;
    protected final Map<Integer, Item> items = new ConcurrentHashMap<>();
    protected final EventBus eventBus = new EventBus("Inventory");

    public Inventory(int maxSlots) {
        this.maxSlots = maxSlots;
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= maxSlots)
            throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds for inventory size " + maxSlots);
    }

    public void set(int slot, Item item) {
        validateSlot(slot);
        items.put(slot, item);
        eventBus.post(new ChangedEvent(this, slot));
    }

    public Item get(int slot) {
        validateSlot(slot);
        return items.getOrDefault(slot, null);
    }

    public void remove(int slot) {
        validateSlot(slot);
        items.remove(slot);
        eventBus.post(new ChangedEvent(this, slot));
    }

    public void setCount(int slot, int count) {
        Item item = get(slot);
        if (item != null) item.setCount(count);
    }

    public int getCount(int slot) {
        Item item = get(slot);
        return item == null ? 0 : item.getCount();
    }

    public void add(int slot, int addition) {
        Item item = get(slot);
        if (item == null) return;

        int newCount = item.getCount() + addition;
        if (newCount <= 0)
            remove(slot);
        else
            item.setCount(newCount);
        eventBus.post(new ChangedEvent(this, slot));
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public boolean isFull() {
        return items.size() >= maxSlots;
    }

    public void clear() {
        items.clear();
        eventBus.post(new ChangedEvent(this, -1));
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public Collection<Item> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public int add(Item item) {
        for (int i = 0; i < maxSlots; i++) {
            Item sel = items.get(i);
            if (sel == null) continue;
            if (sel.getType() == item.getType()) {
                sel.setCount(sel.getCount() + item.getCount());
                return i;
            }
        }
        return 0;
    }

    public void cleanup() {
        items.clear();
    }

    public static class ChangedEvent extends Event {
        public final Inventory inventory;
        public final int slot;

        public ChangedEvent(Inventory inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }
    }
}
