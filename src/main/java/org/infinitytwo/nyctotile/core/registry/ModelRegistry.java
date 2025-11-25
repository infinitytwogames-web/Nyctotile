package org.infinitytwo.nyctotile.core.registry;

import org.infinitytwo.nyctotile.core.model.Model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ModelRegistry {
    private static final Map<Integer, Model> intToModel = new ConcurrentHashMap<>();
    private static final Map<String, Integer> nameToId = new ConcurrentHashMap<>();

    public static int register(Model model) {
        int index = intToModel.size();
        intToModel.put(index, model);
        nameToId.put(model.getId(),index);
        return index;
    }

    public static Model get(int index) {
        return intToModel.get(index);
    }

    public static Set<Map.Entry<Integer, Model>> getEntries() {
        return Collections.unmodifiableSet(intToModel.entrySet());
    }

    public static int getIndex(String name) {
        return nameToId.get(name);
    }

    public static Model get(String name) {
        return get(getIndex(name));
    }
}
