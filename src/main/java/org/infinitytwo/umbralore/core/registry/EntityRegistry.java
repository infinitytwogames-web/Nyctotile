package org.infinitytwo.umbralore.core.registry;

import org.infinitytwo.umbralore.core.entity.Entity;
import org.infinitytwo.umbralore.core.model.TextureAtlas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityRegistry extends Registry<Entity> {
    private static final EntityRegistry registry = new EntityRegistry();
    private final TextureAtlas atlas = new TextureAtlas(50,50);

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public static EntityRegistry getRegistry() {
        return registry;
    }
}
