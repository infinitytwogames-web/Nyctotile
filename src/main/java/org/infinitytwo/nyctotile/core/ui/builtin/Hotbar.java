package org.infinitytwo.nyctotile.core.ui.builtin;

import org.infinitytwo.nyctotile.core.RGBA;
import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.Inventory;
import org.infinitytwo.nyctotile.core.data.Item;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.MouseScrollEvent;
import org.infinitytwo.nyctotile.core.registry.ItemRegistry;
import org.infinitytwo.nyctotile.core.renderer.FontRenderer;
import org.infinitytwo.nyctotile.core.ui.display.Scene;
import org.jetbrains.annotations.NotNull;

public class Hotbar extends InventoryGridViewer {
    protected int
            selected,
            row
    ;
    private RGBA original;

    public Hotbar(Scene renderer, FontRenderer fontRenderer, Window window, int slots) {
        super(renderer, fontRenderer, window, new ItemFactory(), slots);

        EventBus.connect(this);
        rows = 1;
        columns = slots;
        padding = 5;
        space = 10;
    }

    @Override
    public void linkInventory(@NotNull Inventory inventory) {
        linkInventory(inventory, 0);
    }

    public void linkInventory(Inventory inventory, int row) {
        this.link = inventory;
        this.row = row;

        setSize(1, columns);
        clearCells();

        int startIndex = row * columns;
        int endIndex = Math.min(startIndex + columns, link.getMaxSlots());
        int column = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ItemSlot slot = getItemSlot(i);
            slot.setItem(inventory.get(i));
            put(slot, 0, column);
            slots.add(slot);
            column++;
        }

        link.getEventBus().register(this);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        EventBus.disconnect(this);
    }

    @SubscribeEvent
    public void onMouseScroll(MouseScrollEvent e) {
        if (e.y > 0 || e.x > 0) {
            selected = (selected >= columns - 1) ? 0 : selected + 1;
        } else if (e.y < 0 || e.x < 0) {
            selected = (selected <= 0) ? columns - 1 : selected - 1;
        }
    }

    @Override
    @SubscribeEvent
    public void refresh(Inventory.ChangedEvent e) {
        if (slots.isEmpty()) return;
        if (e == null) {
            // refresh all
            int startIndex = this.row * this.columns; // Get the correct starting index
            for (int i = 0; i < columns; i++) {
                slots.get(i).setItem(link.get(startIndex + i)); // Correct slot index
            }
        } else {
            if (link == null || e.slot >= slots.size()) return;
            slots.get(e.slot).setItem(link.get(e.slot));
        }
    }

    private static class ItemFactory implements Factory {

        @Override
        public ItemSlot create(int slot, Item item, Scene scene, FontRenderer fontRenderer, Window window) {
            ItemSlot i = new ItemSlot(scene,fontRenderer,window);

            i.setAtlas(ItemRegistry.getTextureAtlas());
            i.setBackgroundColor(0,0,0.25f,0.5f);

            return i;
        }
    }

    @Override
    public void clearCells() {
        super.clearCells();
        slots.clear();
    }
}
