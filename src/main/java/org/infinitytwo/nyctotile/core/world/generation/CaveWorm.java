package org.infinitytwo.nyctotile.core.world.generation;

import org.infinitytwo.nyctotile.core.data.world.ChunkData;
import org.infinitytwo.nyctotile.core.exception.IllegalChunkAccessException;
import org.infinitytwo.nyctotile.core.world.ServerGridMap;
import org.joml.Vector3i;

import java.util.Random;

@Deprecated
public class CaveWorm {
    public double x;
    public double y;
    public double z;
    double dx, dy, dz;
    public float radius;
    Random rand;

    public CaveWorm(double x, double y, double z, Random rand) {
        this.x = x; this.y = y; this.z = z;
        this.rand = rand;

        // random initial direction
        this.dx = rand.nextDouble() * 2 - 1;
        this.dy = (rand.nextDouble() * 0.4) - 0.2; // mostly horizontal
        this.dz = rand.nextDouble() * 2 - 1;
        normalizeDirection();

        this.radius = 2 + rand.nextFloat() * 2; // tunnel size 2–4
    }

    private void normalizeDirection() {
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        dx /= len; dy /= len; dz /= len;
    }

    public void step() {
        x += dx; y += dy; z += dz;

        // random wiggle
        dx += (rand.nextDouble()-0.5)*0.3;
        dy += (rand.nextDouble()-0.5)*0.05;
        dz += (rand.nextDouble()-0.5)*0.3;

        dy *= 0.9;
        normalizeDirection();
    }

    public void carveEllipsoid(ChunkData grid, Vector3i pos, float radius, float radiusY) throws IllegalChunkAccessException {
        int minX = (int) Math.floor(pos.x - radius);
        int maxX = (int) Math.ceil(pos.x + radius);
        int minY = (int) Math.floor(pos.y - radiusY);
        int maxY = (int) Math.ceil(pos.y + radiusY);
        int minZ = (int) Math.floor(pos.z - radius);
        int maxZ = (int) Math.ceil(pos.z + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // normalize distances
                    float dx = (x + 0.5f - pos.x) / radius;
                    float dy = (y + 0.5f - pos.y) / radiusY;
                    float dz = (z + 0.5f - pos.z) / radius;
                    if (dx*dx + dy*dy + dz*dz < 1.0f) {
                        grid.setBlock(ServerGridMap.convertToLocalChunk(x, y +2, z), (short)0); // carve air
                    }
                }
            }
        }
    }

    public void carveSphere(ChunkData grid, Vector3i pos, float radius) throws IllegalChunkAccessException {
        int minY = (int) (pos.y - radius);
        int maxY = (int) (pos.y + radius);

        float cx = pos.x - 0.5f,
                cy = pos.y - 0.5f,
                cz = pos.z - 0.5f;

        float r2 = radius * radius;

        for (int y = minY; y <= maxY; y++) {
            float dy = y - cy;
            float dy2 = dy * dy;
            float rSlice2 = r2 - dy2;
            if (rSlice2 < 0) continue;

            int rSlice = (int)Math.sqrt(rSlice2);
            for (int x = (int)(cx - rSlice); x <= (int)(cx + rSlice); x++) {
                float dx = x - cx;
                float dx2 = dx * dx;
                float dzMax = (float)Math.sqrt(rSlice2 - dx2);

                int minZ = (int)(cz - dzMax);
                int maxZ = (int)(cz + dzMax);

                for (int z = minZ; z <= maxZ; z++) {
                    int lx = x & (ChunkData.SIZE_X -1), ly = y & (ChunkData.SIZE_Y -1), lz = z & (ChunkData.SIZE_Z -1);

                    grid.setBlock(lx, ly, lz, 0);
                }
            }
        }
    }

    public void carve(ChunkData data, int steps) throws IllegalChunkAccessException {
        for (int i = 0; i < steps; i++) {
            step();

            int cy = (int) Math.floor(y);
            if (cy <= 1 || cy >= ChunkData.SIZE_Y - 1) continue;

            // vary radius per step for natural shape
            float radiusXZ = radius * (0.75f + rand.nextFloat() * 0.5f);
            float radiusY  = radiusXZ * (0.5f + rand.nextFloat() * 0.5f);

            carveEllipsoid(data, new Vector3i((int)x, (int)y, (int)z), radiusXZ, radiusY);
        }
    }
}
