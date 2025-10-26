package org.infinitytwo.umbralore.core.registry;

import org.infinitytwo.umbralore.core.exception.UnknownRegistryException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Registry<T extends Registerable> {
    protected final Map<Integer, T> idToData = new ConcurrentHashMap<>();
    protected final Map<String, Integer> nameToId = new ConcurrentHashMap<>();

    private short nextId = 1;

    public int register(T block) {
        int id = nextId++;
        idToData.put(id, block);
        nameToId.put(block.getId(), id);
        return id;
    }

    public T get(int id) {
        if (idToData.get(id) == null) throw new UnknownRegistryException("Couldn't find a registry with id: " + id);
        return idToData.get(id);
    }

    public T get(String id) {
        return get(getId(id));
    }

    public int getId(String name) {
        return nameToId.get(name);
    }

    public Set<Integer> getIds() {
        return idToData.keySet();
    }

    public Collection<T> getRegistries() {
        return Collections.unmodifiableCollection(idToData.values());
    }

    public int size() {
        return idToData.size();
    }

    public Set<Map.Entry<Integer,T>> getEntries() {
        return idToData.entrySet();
    }
}
