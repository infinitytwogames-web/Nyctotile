package org.infinitytwo.nyctotile.core.manager;

import org.infinitytwo.nyctotile.core.entity.Entity;

import java.util.*;

public class EntityManager {
    private static final List<Entity> entities = Collections.synchronizedList(new ArrayList<>());

    public static void put(Entity entity) {
        entities.add(entity);
    }

    public static Collection<Entity> getAllEntities() {
        return Collections.unmodifiableCollection(entities);
    }
    
    public static Entity getEntityFromId(UUID uuid) {
        synchronized (entities) {
            for (Entity entity : entities) {
                if (entity.getUUID().equals(uuid)) {
                    return entity;
                }
            }
        }
        return null;
    }
}
