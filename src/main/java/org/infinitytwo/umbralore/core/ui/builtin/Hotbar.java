package org.infinitytwo.umbralore.core.ui.builtin;

import org.infinitytwo.umbralore.core.RGBA;
import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.Inventory;
import org.infinitytwo.umbralore.core.data.Item;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.input.MouseScrollEvent;
import org.infinitytwo.umbralore.core.registry.ItemRegistry;
import org.infinitytwo.umbralore.core.renderer.FontRenderer;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.jetbrains.annotations.NotNull;

public class Hotbar extends InventoryViewer {
    protected int
            selected,
            row
    ;
    private RGBA original;

    public Hotbar(Screen renderer, FontRenderer fontRenderer, Window window, int slots) {
        super(renderer, fontRenderer, window, new ItemFactory(), slots);

        EventBus.register(this);
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

        link.getEventBus().register(this);

        setSize(1, columns);

        slots.clear();

        int startIndex = row * columns;
        int endIndex = Math.min(startIndex + columns, link.getMaxSlots());
        for (int i = startIndex; i < endIndex; i++) {
            ItemSlot slot = getItemSlot(i);
            put(slot, 0, i);
            slots.add(slot);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        EventBus.unregister(this);
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
        if (e == null) {
            // refresh all
            for (int i = 0; i < columns; i++) {
                slots.get(i).setItem(link.get(i));
            }
        } else {
            if (link == null || e.slot >= slots.size()) return;
            slots.get(e.slot).setItem(link.get(e.slot));
        }
    }

    private static class ItemFactory implements Factory {

        @Override
        public ItemSlot create(int slot, Item item, Screen screen, FontRenderer fontRenderer, Window window) {
            ItemSlot i = new ItemSlot(screen,fontRenderer,window);

            i.setAtlas(ItemRegistry.getTextureAtlas());
            i.setBackgroundColor(0,0,0.25f,0.5f);

            return i;
        }
    }
}
