package org.infinitytwo.nyctotile.core.registry;

import org.infinitytwo.nyctotile.core.data.ItemType;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// CHANGE 1: Extend the base Registry class
public class ItemRegistry extends Registry<ItemType> {

    // Kept for item-specific data: the texture map
    private final Map<Integer, Integer> idToTexture = new ConcurrentHashMap<>();

    private static final TextureAtlas atlas = new TextureAtlas(12, 10);
    private static final ItemRegistry registry = new ItemRegistry();

    public static TextureAtlas getTextureAtlas() {
        return atlas;
    }

    public static ItemRegistry getMainRegistry() {
        return registry;
    }

    public int register(ItemType item, int textureIndex) {
        int index = super.register(item);
        idToTexture.put(index, textureIndex);
        item.setIndex(index);

        return index;
    }

    // NOTE: The 'get(int)' and 'get(String)' methods can be DELETED
    // because they are inherited from the base Registry class,
    // which already handles the UnknownRegistryException!

    // NOTE: The 'getEntries()' method can be DELETED as it's inherited.

    // The existing explicit registration method for world loading is kept,
    // but simplified to call the superclass's method.
    // CHANGE 3: Simplified registerIndexed to use the base class's register method
    public void registerIndexed(int id, ItemType type) {
        super.register(id, type);
    }

    // Kept: Item-specific lookup
    public int getTextureIndex(int id) {
        return idToTexture.getOrDefault(id, -1);
    }
}