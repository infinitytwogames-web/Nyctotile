package org.infinitytwo.nyctotile.core.registry;

import org.infinitytwo.nyctotile.core.entity.Entity;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;

public class EntityRegistry extends Registry<Entity> {
    private static final EntityRegistry registry = new EntityRegistry();
    private final TextureAtlas atlas = new TextureAtlas(50,50);

    public static EntityRegistry getRegistry() {
        return getMainRegistry();
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public static EntityRegistry getMainRegistry() {
        return registry;
    }
}
