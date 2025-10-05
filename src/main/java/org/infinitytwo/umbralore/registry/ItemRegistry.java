package org.infinitytwo.umbralore.registry;

import org.infinitytwo.umbralore.data.ItemType;
import org.infinitytwo.umbralore.exception.UnknownRegistryException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRegistry {
    protected final Map<Integer, ItemType> idToItem = new ConcurrentHashMap<>();
    protected final Map<String, ItemType> nameToItem = new ConcurrentHashMap<>();

    public void register(ItemType item) {
        idToItem.put(idToItem.size(),item);
        nameToItem.put(item.getName().toString(),item);
    }

    public ItemType get(int id) throws UnknownRegistryException {
        ItemType type = idToItem.getOrDefault(id, null);
        if (type == null) throw new UnknownRegistryException("Couldn't find item with id "+id+".");
        else return type;
    }
}
