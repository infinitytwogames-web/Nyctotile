package org.infinitytwo.nyctotile.core.ui.component;

import org.infinitytwo.nyctotile.core.RGB;
import org.infinitytwo.nyctotile.core.data.ItemType;
import org.infinitytwo.nyctotile.core.data.Item;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.registry.ItemRegistry;
import org.infinitytwo.nyctotile.core.renderer.FontRenderer;
import org.infinitytwo.nyctotile.core.ui.display.Scene;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.joml.Vector2i;

public class ItemHolder extends TextureComponent {
    private final ItemRegistry registry;
    private final Text text;
    private Item item;
    private final int padding = 2;

    public ItemHolder(TextureAtlas atlas, Scene scene, int index, FontRenderer fontRenderer) {
        super(index, atlas, scene.getUIBatchRenderer());
        this.registry = null;

        text = new Text(fontRenderer, scene);
        text.setParent(this);
        text.setPosition(new Anchor(0, 1), new Pivot(0, 1), new Vector2i(padding, -padding));
        text.setColor(new RGB(0.75f,0.75f,0.75f));
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        if (item == null) {
            setTextureIndex(-1);
            text.setText("");
            this.item = null;
            return;
        }

        this.item = item;
    }

    public ItemType getType() {
        return item.getType();
    }

    public int getDurability() {
        return item.getDurability();
    }

    public void setDurability(int durability) {
        item.setDurability(durability);
    }

    public int getCount() {
        return item.getCount();
    }

    public void setCount(int count) {
        item.setCount(count);
    }

    public void damage(int amount) {
        item.damage(amount);
    }

    public void repair(int amount) {
        item.repair(amount);
    }

    public boolean isBroken() {
        return item.isBroken();
    }

    public boolean isStackable() {
        return item.isStackable();
    }

    @Override
    public void draw() {
        if (item == null) {
            return;
        }
        text.setText(item.getCount() == 1? "" : String.valueOf(item.getCount()));
        super.draw();
        text.draw();
    }

    public void renderUsing(TextureAtlas atlas, int index) {
        setTextureIndex(index);
        setAtlas(atlas);
    }

    public Scene getScreen() {
        return text.scene;
    }
}
