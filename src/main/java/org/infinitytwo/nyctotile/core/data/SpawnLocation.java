package org.infinitytwo.nyctotile.core.data;

import org.infinitytwo.nyctotile.core.world.dimension.Dimension;
import org.joml.Vector3f;

public record SpawnLocation(Vector3f position, Dimension dimension) {
}
