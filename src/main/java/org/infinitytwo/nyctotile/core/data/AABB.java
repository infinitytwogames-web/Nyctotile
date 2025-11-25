package org.infinitytwo.nyctotile.core.data;

import org.joml.Vector3f;

public class AABB {
    public float minX, minY, minZ,
                 maxX, maxY, maxZ;

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Checks if this AABB intersects (overlaps) with another AABB.
     * * This relies on the simple Separating Axis Theorem (SAT):
     * Two boxes do NOT intersect if they are separated on ANY single axis.
     * Therefore, they DO intersect if they overlap on ALL THREE axes (X, Y, and Z).
     * * @param other The other AABB to check against.
     * @return true if the boxes overlap, false otherwise.
     */
    public boolean isIntersecting(AABB other) {
        // Check for separation on the X-axis
        if (this.maxX <= other.minX || this.minX >= other.maxX) {
            return false;
        }

        // Check for separation on the Y-axis
        if (this.maxY <= other.minY || this.minY >= other.maxY) {
            return false;
        }

        // Check for separation on the Z-axis
        if (this.maxZ <= other.minZ || this.minZ >= other.maxZ) {
            return false;
        }

        // If no separation was found on any axis, the boxes must intersect.
        return true;
    }

    /**
     * Creates and returns a NEW AABB that is translated (moved) by the given
     * offset amounts. This is used to place a block-type AABB (like a slab)
     * at its correct world position (x, y, z).
     *
     * @param x The amount to offset along the X-axis (usually the block's world X coordinate).
     * @param y The amount to offset along the Y-axis.
     * @param z The amount to offset along the Z-axis.
     * @return A new AABB instance at the world position.
     */
    public AABB offset(float x, float y, float z) {
        return new AABB(
                this.minX + x, this.minY + y, this.minZ + z,
                this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    public AABB offset(Vector3f pos) {
        return offset(pos.x, pos.y, pos.z);
    }
}
