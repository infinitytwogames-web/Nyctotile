package org.infinitytwo.nyctotile.core.data;

import org.joml.Vector3i;

public record RaycastResult(Vector3i blockPos, Vector3i hitNormal){}
