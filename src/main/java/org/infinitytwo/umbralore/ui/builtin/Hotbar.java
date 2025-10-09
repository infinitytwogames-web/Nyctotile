package org.infinitytwo.umbralore.ui.builtin;

import org.infinitytwo.umbralore.RGBA;
import org.infinitytwo.umbralore.data.Inventory;
import org.infinitytwo.umbralore.event.SubscribeEvent;
import org.infinitytwo.umbralore.event.bus.EventBus;
import org.infinitytwo.umbralore.event.input.MouseScrollEvent;
import org.infinitytwo.umbralore.renderer.FontRenderer;
import org.infinitytwo.umbralore.ui.Screen;
import org.jetbrains.annotations.NotNull;

import static org.infinitytwo.umbralore.AdvancedMath.clamp;

public class Hotbar extends InventoryViewer {
    protected int
            selected,
            row
    ;
    private RGBA original;

    public Hotbar(Screen renderer, FontRenderer fontRenderer, int slots) {
        super(renderer, fontRenderer, slots);

        EventBus.register(this);
        rows = 1;
        columns = slots;
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
            ItemSlot slot = new ItemSlot(screen, fontRenderer);
            slot.setItem(link.get(i));
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
    public void draw() {
        for (ItemSlot slot : slots)
            slot.setBackgroundColor(original == null ? slot.getBackgroundColor() : original);

        int index = row * columns + selected;
        if (index < 0 || index >= slots.size()) return;

        ItemSlot slot = slots.get(index);
        original = (original == null) ? slot.getBackgroundColor() : original;
        slot.setBackgroundColor(
                clamp(original.r() + 0.2f, 0f, 1f),
                clamp(original.g() + 0.2f, 0f, 1f),
                clamp(original.b() + 0.2f, 0f, 1f),
                original.a()
        );

        super.draw();
    }
}
