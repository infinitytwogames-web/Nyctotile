package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.world.dimension.Dimension;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class World {
    private static Dimension dimension;
    private static Map<String, Dimension> loadedDimension = new HashMap<>();
    private static int seed;

    public static Dimension getCurrentDimension() {
        return dimension;
    }

    public static void setCurrentDimension(String dimension) {
        World.dimension = getLoadedDimension(dimension);
    }

    public static Dimension getLoadedDimension(String dimension) {
        return loadedDimension.get(dimension);
    }

    public static void loadDimension(Dimension dimension) {
        loadedDimension.put(dimension.getId(),dimension);
    }

    public static Collection<Dimension> getLoadedDimensions() {
        return Collections.unmodifiableCollection(loadedDimension.values());
    }

    public static int getSeed() {
        return seed;
    }

    public static void setSeed(int seed) {
        World.seed = seed;
    }
}
