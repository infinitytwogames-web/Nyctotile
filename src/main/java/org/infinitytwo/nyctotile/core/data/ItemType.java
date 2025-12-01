package org.infinitytwo.nyctotile.core.data;

import org.infinitytwo.nyctotile.core.constants.Material;
import org.infinitytwo.nyctotile.core.registry.Registerable;

public class ItemType implements Registerable {
    private final Item.ItemBehaviour itemBehaviour;
    private final boolean isEatable, isDrinkable, fireResistant;
    private final float nutrients, saturation;
    private final Recipe recipe;
    private final int maxDurability;
    private final Material material;
    private final String name;
    private final String id;
    private transient int index;

    private ItemType(String id, Item.ItemBehaviour itemBehaviour, boolean isEatable, boolean isDrinkable,
                    boolean fireResistant, float nutrients, float saturation,
                    Recipe recipe, int maxDurability, Material material, String name) {
        this.itemBehaviour = itemBehaviour;
        this.isEatable = isEatable;
        this.isDrinkable = isDrinkable;
        this.fireResistant = fireResistant;
        this.nutrients = nutrients;
        this.saturation = saturation;
        this.recipe = recipe;
        this.maxDurability = maxDurability;
        this.material = material;
        this.name = name;
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return "";
    }

    public static final class Builder {
        private Item.ItemBehaviour itemBehaviour;
        private boolean isEatable, isDrinkable, fireResistant;
        private float nutrients, saturation;
        private Recipe recipe;
        private int maxDurability;
        private Material material;
        private String name;
        private String id;

        public Builder type(Item.ItemBehaviour itemBehaviour) { this.itemBehaviour = itemBehaviour; return this; }
        public Builder isEatable(boolean eatable) { this.isEatable = eatable; return this; }
        public Builder fireResistant(boolean fr) { this.fireResistant = fr; return this; }
        public Builder isDrinkable(boolean drinkable) { this.isDrinkable = drinkable; return this; }
        public Builder nutrients(float n) { this.nutrients = n; return this; }
        public Builder saturation(float s) { this.saturation = s; return this; }
        public Builder maxDurability(int md) { this.maxDurability = md; return this; }
        public Builder material(Material material) { this.material = material; return this; }
        public Builder recipe(Recipe recipe) { this.recipe = recipe; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder id(String name) { this.id = name; return this; }

        public ItemType build() {
            return new ItemType(id, itemBehaviour, isEatable, isDrinkable, fireResistant,
                    nutrients, saturation, recipe,
                    maxDurability, material, name);
        }
    }

    public Item.ItemBehaviour getType() {
        return itemBehaviour;
    }

    public boolean isEatable() {
        return isEatable;
    }

    public boolean isDrinkable() {
        return isDrinkable;
    }

    public boolean isFireResistant() {
        return fireResistant;
    }

    public float getNutrients() {
        return nutrients;
    }

    public float getSaturation() {
        return saturation;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isConsumable() {
        return isEatable || isDrinkable;
    }

    public boolean isDurable() {
        return maxDurability > 0;
    }
}
