package org.infinitytwo.nyctotile.core.model.builder;

import org.infinitytwo.nyctotile.core.data.AABB;
import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;

import java.nio.FloatBuffer;

public final class ModelBuilder {
    private final float minX, minY, minZ,
        maxX, maxY, maxZ;

    public ModelBuilder(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public ModelBuilder(AABB aabb) {
        minX = aabb.minX;
        minY = aabb.minY;
        minZ = aabb.minZ;
        maxX = aabb.maxX;
        maxY = aabb.maxY;
        maxZ = aabb.maxZ;
    }

    public void addFace(Face face, float[] uv, NFloatBuffer buffer) {
        // Extract UV coordinates (assuming uv is [u0, v0, u1, v1])
        float u0 = uv[0]; // uMin
        float v0 = uv[1]; // vMin
        float u1 = uv[2]; // uMax
        float v1 = uv[3]; // vMax
        float b; // Brightness variable

        // CRITICAL: Ensure capacity for a full face (6 vertices * 6 floats/vertex = 36 floats)
        buffer.require(36);

        switch (face) {
            case NORTH -> { // -Z (Correct - North Face uses 0.7f brightness)
                b = 0.7f;
                // tri 1 (000, 100, 110)
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (000, 110, 010)
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case SOUTH -> { // +Z (South Face - uses 0.7f brightness)
                b = 0.7f;
                // tri 1 (101, 001, 011)
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (101, 011, 111)
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case WEST -> { // -X (West Face - uses 0.7f brightness)
                b = 0.7f;
                // tri 1 (001, 000, 010)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (001, 010, 011)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case EAST -> { // +X (East Face - uses 0.8f brightness)
                b = 0.8f;
                // tri 1 (100, 101, 111)
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (100, 111, 110)
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case UP -> { // +Y (Up Face - uses 0.8f brightness)
                b = 0.8f;
                // tri 1 (010, 110, 111)
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (010, 111, 011)
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case DOWN -> { // -Y (Down Face - uses 0.5f brightness)
                b = 0.5f;
                // tri 1 (001, 101, 100)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (001, 100, 000)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
        }
    }

    public void addFace(Face face, float[] uv, FloatBuffer buffer) {
        // Extract UV coordinates (assuming uv is [u0, v0, u1, v1])
        float u0 = uv[0]; // uMin
        float v0 = uv[1]; // vMin
        float u1 = uv[2]; // uMax
        float v1 = uv[3]; // vMax
        float b; // Brightness variable

        switch (face) {
            case NORTH -> { // -Z (Correct - North Face uses 0.7f brightness)
                b = 0.7f;
                // tri 1 (000, 100, 110)
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (000, 110, 010)
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case SOUTH -> { // +Z (South Face - uses 0.7f brightness)
                b = 0.7f;
                // tri 1 (101, 001, 011)
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (101, 011, 111)
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case WEST -> { // -X (West Face - uses 0.7f brightness)
                b = 0.7f;
                // tri 1 (001, 000, 010)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (001, 010, 011)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case EAST -> { // +X (East Face - uses 0.8f brightness)
                b = 0.8f;
                // tri 1 (100, 101, 111)
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (100, 111, 110)
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case UP -> { // +Y (Up Face - uses 0.8f brightness)
                b = 0.8f;
                // tri 1 (010, 110, 111)
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(minZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (010, 111, 011)
                buffer.put(minX); buffer.put(maxY); buffer.put(minZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(maxY); buffer.put(maxZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
            case DOWN -> { // -Y (Down Face - uses 0.5f brightness)
                b = 0.5f;
                // tri 1 (001, 101, 100)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(maxZ); buffer.put(u1); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);

                // tri 2 (001, 100, 000)
                buffer.put(minX); buffer.put(minY); buffer.put(maxZ); buffer.put(u0); buffer.put(v0); buffer.put(b);
                buffer.put(maxX); buffer.put(minY); buffer.put(minZ); buffer.put(u1); buffer.put(v1); buffer.put(b);
                buffer.put(minX); buffer.put(minY); buffer.put(minZ); buffer.put(u0); buffer.put(v1); buffer.put(b);
            }
        }
    }

    public void cube(NFloatBuffer buffer, float[] uvs) {
        addFace(Face.UP,uvs,buffer);
        addFace(Face.DOWN,uvs,buffer);
        addFace(Face.NORTH,uvs,buffer);
        addFace(Face.SOUTH,uvs,buffer);
        addFace(Face.EAST,uvs,buffer);
        addFace(Face.WEST,uvs,buffer);
    }
}
