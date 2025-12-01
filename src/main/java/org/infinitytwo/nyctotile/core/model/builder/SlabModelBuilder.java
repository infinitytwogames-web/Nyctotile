package org.infinitytwo.nyctotile.core.model.builder;

import org.infinitytwo.nyctotile.core.constants.Face;
import org.infinitytwo.nyctotile.core.data.world.AABB;
import org.infinitytwo.nyctotile.core.world.GridMap;

import java.util.ArrayList;

public class SlabModelBuilder {
    private static final float minY = 0f,
            maxY = 0.5f, // <-- Slab height
            minX = 0f,
            maxX = 1f,
            minZ = 0f,
            maxZ = 1f;

    private static void addFace(Face face, int x, int y, int z, float[] uv, ArrayList<Float> vertices) {
        float u0 = uv[0]; // uMin
        float v0 = uv[1]; // vMin
        float u1 = uv[2]; // uMax
        float v1 = uv[3]; // vMax

        switch (face) {
            case NORTH -> { // -Z (Correctly using minY and maxY)
                // tri 1
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.7f);
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v0); vertices.add(0.7f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.7f);

                // tri 2
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.7f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v1); vertices.add(0.7f);
            }
            case SOUTH -> { // +Z (Adjusted Y coordinates)
                // tri 1
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v0); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.7f);

                // tri 2
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.7f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v1); vertices.add(0.7f);
            }
            case WEST -> { // -X (Adjusted Y coordinates)
                // tri 1
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v0); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.7f);

                // tri 2
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.7f);
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v1); vertices.add(0.7f);
            }
            case EAST -> { // +X (Adjusted Y coordinates)
                // tri 1
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.8f);
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v0); vertices.add(0.8f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.8f);

                // tri 2
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.8f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.8f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v1); vertices.add(0.8f);
            }
            case UP -> { // +Y (Adjusted Y coordinates)
                // This face is drawn at the maxY = 0.5f plane
                // tri 1
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.8f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v0); vertices.add(0.8f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.8f);

                // tri 2
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.8f);
                vertices.add(maxX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.8f);
                vertices.add(minX+x); vertices.add(maxY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v1); vertices.add(0.8f);
            }
            case DOWN -> { // -Y (Correctly using minY)
                // This face is drawn at the minY = 0.0f plane
                // tri 1
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.5f);
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u1); vertices.add(v0); vertices.add(0.5f);
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.5f);

                // tri 2
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(maxZ+z); vertices.add(u0); vertices.add(v0); vertices.add(0.5f);
                vertices.add(maxX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u1); vertices.add(v1); vertices.add(0.5f);
                vertices.add(minX+x); vertices.add(minY+y); vertices.add(minZ+z); vertices.add(u0); vertices.add(v1); vertices.add(0.5f);
            }
        }
    }

    public static AABB[] getBoundingBoxes() {
        return new AABB[]{new AABB(minX,minY,minZ,maxX,maxY,maxZ)};
    }

    public static void buildSlabVertices(GridMap map, int x, int y, int z,float[] uv, ArrayList<Float> b) {
        if (!map.isTransparent(x, y - 1, z)) addFace(Face.DOWN,x,y,z,uv,b);
        if (!map.isTransparent(x + 1, y, z)) addFace(Face.EAST,x,y,z,uv,b);
        if (!map.isTransparent(x - 1, y, z)) addFace(Face.WEST,x,y,z,uv,b);
        if (!map.isTransparent(x, y, z + 1)) addFace(Face.SOUTH,x,y,z,uv,b);
        if (!map.isTransparent(x, y, z - 1)) addFace(Face.NORTH,x,y,z,uv,b);
    }
}
