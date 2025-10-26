package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class EntityManager {
    private static final ArrayList<Entity> entities = new ArrayList<>();

    public static void put(Entity entity) {
        entities.add(entity);
    }

    public static Collection<Entity> getAllEntities() {
        return Collections.unmodifiableCollection(entities);
    }
}
