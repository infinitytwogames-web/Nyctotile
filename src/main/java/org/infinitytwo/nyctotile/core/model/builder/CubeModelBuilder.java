package org.infinitytwo.nyctotile.core.model.builder;

import org.infinitytwo.nyctotile.core.constants.Face;
import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;
import org.infinitytwo.nyctotile.core.world.GridMap;

public class CubeModelBuilder {
    private static void addFace(Face face, int x, int y, int z, float[] uv, NFloatBuffer buffer, RGBA light) {
        // Extract UV coordinates (assuming uv is [u0, v0, u1, v1])
        float u0 = uv[0]; // uMin
        float v0 = uv[1]; // vMin
        float u1 = uv[2]; // uMax
        float v1 = uv[3]; // vMax
        float b; // Brightness variable

        // CRITICAL: Ensure capacity for a full face (6 vertices * 6 floats/vertex = 36 floats)
        buffer.require(36 + (4 * 6));

        switch (face) {
            case NORTH -> { // -Z (Correct - North Face uses 0.7f brightness)
                b = 0.2f;
                // tri 1 (000, 100, 110)
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());

                // tri 2 (000, 110, 010)
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
            }
            case SOUTH -> { // +Z (South Face - uses 0.7f brightness)
                b = 0.3f;
                // tri 1 (101, 001, 011)
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());

                // tri 2 (101, 011, 111)
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
            }
            case WEST -> { // -X (West Face - uses 0.7f brightness)
                b = 0.3f;
                // tri 1 (001, 000, 010)
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());

                // tri 2 (001, 010, 011)
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
            }
            case EAST -> { // +X (East Face - uses 0.8f brightness)
                b = 0.4f;
                // tri 1 (100, 101, 111)
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());

                // tri 2 (100, 111, 110)
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
            }
            case UP -> { // +Y (Up Face - uses 0.8f brightness)
                b = 0.4f;
                // tri 1 (010, 110, 111)
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());

                // tri 2 (010, 111, 011)
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(1f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
            }
            case DOWN -> { // -Y (Down Face - uses 0.5f brightness)
                b = 0.1f;
                // tri 1 (001, 101, 100)
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u1); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());

                // tri 2 (001, 100, 000)
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(1f+z); buffer.put(u0); buffer.put(v0); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(1f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u1); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
                buffer.put(0f+x); buffer.put(0f+y); buffer.put(0f+z); buffer.put(u0); buffer.put(v1); buffer.put(b); buffer.put(light.getRed()); buffer.put(light.getGreen()); buffer.put(light.getBlue()); buffer.put(light.getAlpha());
            }
        }
    }

    public static void standardVerticesList(GridMap map, int x, int y, int z, float[] uvs, RGBA light, NFloatBuffer buffer) {
        if (map.isTransparent(x, y+1, z)) addFace(Face.UP, x, y, z, uvs, buffer, light);
        if (map.isTransparent(x, y-1, z)) addFace(Face.DOWN, x, y, z, uvs, buffer, light);
        if (map.isTransparent(x+1, y, z)) addFace(Face.EAST, x, y, z, uvs, buffer, light);
        if (map.isTransparent(x-1, y, z)) addFace(Face.WEST, x, y, z, uvs, buffer, light);
        if (map.isTransparent(x, y, z+1)) addFace(Face.SOUTH, x, y, z, uvs, buffer, light);
        if (map.isTransparent(x, y, z-1)) addFace(Face.NORTH, x, y, z, uvs, buffer, light);
    }
}