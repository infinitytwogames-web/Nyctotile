package org.infinitytwo.umbralore.core.registry;

import org.infinitytwo.umbralore.core.data.ItemType;
import org.infinitytwo.umbralore.core.exception.UnknownRegistryException;
import org.infinitytwo.umbralore.core.model.TextureAtlas;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRegistry {
    private final Map<Integer, ItemType> idToItem = new ConcurrentHashMap<>();
    private final Map<String, ItemType> nameToItem = new ConcurrentHashMap<>();
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
        int index = idToItem.size();
        item.setIndex(index);

        idToItem.put(index, item);
        nameToItem.put(item.getName().toString(), item);
        idToTexture.put(index, textureIndex);

        return index;
    }

    public ItemType get(int id) throws UnknownRegistryException {
        ItemType type = idToItem.get(id);
        if (type == null)
            throw new UnknownRegistryException("Couldn't find item with id " + id + ".");
        return type;
    }

    public ItemType get(String name) throws UnknownRegistryException {
        ItemType type = nameToItem.get(name);
        if (type == null)
            throw new UnknownRegistryException("Couldn't find item with name " + name + ".");
        return type;
    }

    public int getTextureIndex(int id) {
        return idToTexture.getOrDefault(id, -1);
    }

    public int size() {
        return idToItem.size();
    }

    public Set<Map.Entry<Integer,ItemType>> getEntries() {
        return Collections.unmodifiableSet(idToItem.entrySet());
    }
}