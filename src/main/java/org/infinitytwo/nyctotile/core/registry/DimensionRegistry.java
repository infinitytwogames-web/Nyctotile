package org.infinitytwo.nyctotile.core.registry;

import org.infinitytwo.nyctotile.core.world.dimension.Dimension;

public class DimensionRegistry extends Registry<Dimension> {
    private static final DimensionRegistry registry = new DimensionRegistry();

    public static DimensionRegistry getRegistry() {
        return registry;
    }
}
