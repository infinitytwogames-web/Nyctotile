package org.infinitytwo.umbralore.core.registry;

import org.infinitytwo.umbralore.core.world.dimension.Dimension;

public class DimensionRegistry extends Registry<Dimension> {
    private static final DimensionRegistry registry = new DimensionRegistry();

    public static DimensionRegistry getRegistry() {
        return registry;
    }
}
