package org.infinitytwo.umbralore.core.data;

import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.joml.Vector3f;

public record SpawnLocation(Vector3f position, Dimension dimension) {
}
