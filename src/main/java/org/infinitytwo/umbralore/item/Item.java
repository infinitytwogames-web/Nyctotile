package org.infinitytwo.umbralore.item;

import org.infinitytwo.umbralore.data.ItemType;

public class Item {
    protected final ItemType type;
    protected int durability;
    protected int count;

    private Item(ItemType type) {
        this.type = type;
        this.durability = type.getMaxDurability(); // defaults to full durability
        this.count = 1; // start with one item by default
    }

    public static Item of(ItemType type) {
        return new Item(type);
    }

    public ItemType getType() {
        return type;
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void damage(int amount) {
        if (type.getMaxDurability() > 0) {
            durability -= amount;
            if (durability < 0) durability = 0;
        }
    }

    public void repair(int amount) {
        if (type.getMaxDurability() > 0) {
            durability = Math.min(durability + amount, type.getMaxDurability());
        }
    }

    public boolean isBroken() {
        return type.getMaxDurability() > 0 && durability <= 0;
    }

    public boolean isStackable() {
        return type.getMaxDurability() == 0; // tools usually not stackable
    }

    public enum ItemBehaviour {
        FOOD,
        TOOL,
        ITEM,
        VALUABLE,
        BLOCK
    }
}
