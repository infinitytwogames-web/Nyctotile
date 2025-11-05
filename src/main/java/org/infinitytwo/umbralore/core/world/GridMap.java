package org.infinitytwo.umbralore.core.world;

import org.infinitytwo.umbralore.block.BlockType;
import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.Block;
import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.data.RaycastResult;
import org.infinitytwo.umbralore.core.data.io.BlockDataReader;
import org.infinitytwo.umbralore.core.exception.IllegalChunkAccessException;
import org.infinitytwo.umbralore.core.exception.IllegalDataTypeException;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.renderer.Camera;
import org.infinitytwo.umbralore.core.renderer.Chunk;
import org.infinitytwo.umbralore.core.renderer.FrustumCuller;
import org.infinitytwo.umbralore.core.renderer.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinitytwo.umbralore.core.data.ChunkData.SIZE_X;
import static org.infinitytwo.umbralore.core.renderer.Chunk.SIZE_Z;

public class GridMap extends GMap {
    protected final ConcurrentHashMap<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();
    protected final BlockRegistry registry;
    private final FrustumCuller culler = new FrustumCuller();

    public GridMap(BlockRegistry registry) {
        this.registry = registry;
    }

    public static Vector2i convertToChunkPosition(Vector3i block) {
        return new Vector2i(
                Math.floorDiv(block.x, SIZE_X),
                Math.floorDiv(block.z, SIZE_Z)
        );
    }

    public static Vector2i convertToChunkPosition(int x, int z) {
        return new Vector2i(
                Math.floorDiv(x, SIZE_X),
                Math.floorDiv(z, SIZE_Z)
        );
    }

    public static Vector3i convertToLocalChunk(int worldX, int worldY, int worldZ) {
        int localX = Math.floorMod(worldX, SIZE_X);
        int localZ = Math.floorMod(worldZ, SIZE_Z);
        return new Vector3i(localX, worldY, localZ);
    }

    public static Vector2i worldToChunk(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, SIZE_Z);
        return new Vector2i(chunkX, chunkZ);
    }


    public static Vector3i convertToWorldPosition(Vector2i chunk, Vector3i localChunkPos) {
        return localChunkPos.add((chunk.x * SIZE_X),0,(chunk.y * SIZE_Z));
    }

    public static Vector3i convertToWorldPosition(Vector2i chunk, int x, int y, int z) {
        return convertToWorldPosition(chunk,new Vector3i(x,y,z));
    }

    public static Vector2i worldToChunk(Vector3i pos) {
        return worldToChunk(pos.x, pos.z);
    }

    public Block getBlock(int x, int y, int z) {
        Vector2i p = convertToChunkPosition(x,z);
        ChunkPos pos = new ChunkPos(p.x, p.y);

        if (chunks.containsKey(pos)) {
            int id = chunks.get(pos).getBlockId(convertToLocalChunk(x,y,z));
            if (id != 0) return new Block(registry.get(id)); else {
                return null;
            }
        } else return null;
    }

    public void removeBlock(int x, int y, int z) throws IllegalChunkAccessException {
        Vector2i p = convertToChunkPosition(x,z);
        ChunkPos pos = new ChunkPos(p.x, p.y);

        if (chunks.containsKey(pos)) {
            chunks.get(pos).setBlock(convertToLocalChunk(x,y,z), 0);
        } else throw new IllegalChunkAccessException("Cannot modify a non-existing chunk. "+p);
    }

    public void removeBlock(Vector3i pos) throws IllegalChunkAccessException {
        removeBlock(pos.x,pos.y, pos.z);
    }
    
    public List<ChunkPos> getSurroundingChunks(ChunkPos center, int radius) {
        List<ChunkPos> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                result.add(new ChunkPos(center.x() + dx, center.z() + dz));
            }
        }
        return result;
    }
    
    @Override
    public BlockType getBlockType(Vector3i pos) {
        return null;
    }
    
    public void insertData(Vector3i pos, byte[] data) throws IllegalChunkAccessException {
        chunks.get(worldToChunkPos(pos.x, pos.z)).setData(pos, data);
    }
    
    @Override
    public byte[] getData(Vector3i pos) throws IllegalChunkAccessException {
        return chunks.get(worldToChunkPos(pos.x, pos.z)).getData(pos);
    }
    
    @Override
    public Object getData(Vector3i pos, BlockDataReader reader, String name) throws IllegalChunkAccessException, IllegalDataTypeException {
        Chunk chunk = chunks.get(worldToChunkPos(pos.x, pos.z));
        return reader.getData(chunk.getBlockId(pos.x,pos.y,pos.z), chunk.getData(pos), name);
    }
    
    public void addChunk(Chunk chunk) {
        chunks.put(new ChunkPos(chunk.getPosition().x,chunk.getPosition().y),chunk);
    }

    public void rebuildChunk(int x, int y) {
//        if (!isReady) return;
        ChunkPos pos = new ChunkPos(x,y);

        if (chunks.containsKey(pos)) {
            chunks.get(pos).dirty();
        }
    }

    public void draw(Camera camera, Window window, int view) {
        // Calculate the combined View-Projection matrix
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(camera.getFov()), (float) window.getSize().x / window.getSize().y, 0.1f, 1024f);
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f viewProjection = projection.mul(viewMatrix, new Matrix4f());

        // 1. Update the Frustum Planes once per frame
//        culler.update(viewProjection);

        // Determine chunks to potentially check (culling region)
        Vector2i chunkP = convertToChunkPosition((int) camera.getPosition().x, (int) camera.getPosition().z);
        ChunkPos pos = new ChunkPos(chunkP.x, chunkP.y);
        List<ChunkPos> r = getSurroundingChunks(pos, view);

        // Chunk AABB corners (y is always fixed)
        final float MIN_Y = 0;
        final float MAX_Y = Chunk.SIZE_Y;

        for (ChunkPos search : r) {
            if (chunks.containsKey(search)) {
                Chunk chunk = chunks.get(search);

//                 2. Define the Chunk's AABB World Coordinates
//                float minX = search.x * Chunk.SIZE_X;
//                float minZ = search.z * Chunk.SIZE_Z;
//                float maxX = minX + Chunk.SIZE_X;
//                float maxZ = minZ + Chunk.SIZE_Z;
//
//                Vector3f min = new Vector3f(minX, MIN_Y, minZ);
//                Vector3f max = new Vector3f(maxX, MAX_Y, maxZ);
//
                // 3. Frustum Culling Test
//                if (culler.isVisible(min, max)) {
                    chunk.draw(camera, window);
//                }
            }
        }
    }

    public RaycastResult raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        Vector3f dir = new Vector3f(direction).normalize(); // Normalized direction
        final float EPSILON = 1e-6f;

        Vector3f pos = new Vector3f(origin);
        Vector3i blockPos = new Vector3i((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        Vector3f step = new Vector3f(Math.signum(dir.x), Math.signum(dir.y), Math.signum(dir.z));

        Vector3f tDelta = new Vector3f(
                Math.abs(dir.x) > EPSILON ? Math.abs(1.0f / dir.x) : Float.POSITIVE_INFINITY,
                Math.abs(dir.y) > EPSILON ? Math.abs(1.0f / dir.y) : Float.POSITIVE_INFINITY,
                Math.abs(dir.z) > EPSILON ? Math.abs(1.0f / dir.z) : Float.POSITIVE_INFINITY
        );

        Vector3f tMax = new Vector3f(
                dir.x > 0 ? ((blockPos.x + 1) - pos.x) * tDelta.x : (pos.x - blockPos.x) * tDelta.x,
                dir.y > 0 ? ((blockPos.y + 1) - pos.y) * tDelta.y : (pos.y - blockPos.y) * tDelta.y,
                dir.z > 0 ? ((blockPos.z + 1) - pos.z) * tDelta.z : (pos.z - blockPos.z) * tDelta.z
        );

        float distance = 0.0f;
        Vector3i normal = new Vector3i();

        while (distance <= maxDistance) {
            Block block = getBlock(blockPos.x, blockPos.y, blockPos.z);

            if (block != null) {
                return new RaycastResult(new Vector3i(blockPos), new Vector3i(normal));
            }

            if (tMax.x < tMax.y && tMax.x < tMax.z) {
                blockPos.x += (int) step.x;
                distance = tMax.x;
                tMax.x += tDelta.x;
                normal.set((int) -step.x, 0, 0);
            } else if (tMax.y < tMax.z) {
                blockPos.y += (int) step.y;
                distance = tMax.y;
                tMax.y += tDelta.y;
                normal.set(0, (int) -step.y, 0);
            } else {
                blockPos.z += (int) step.z;
                distance = tMax.z;
                tMax.z += tDelta.z;
                normal.set(0, 0, (int) -step.z);
            }
        }

        return null;
    }
    
    @Override
    public void setBlock(Block block) throws IllegalChunkAccessException {
    
    }
    
    public void placeBlock(Block block) throws IllegalChunkAccessException {
        Vector3i blockPos = block.getPosition();
        Vector2i p = convertToChunkPosition(blockPos);
        ChunkPos pos = new ChunkPos(p.x, p.y);

        if (chunks.containsKey(pos)) {
            chunks.get(pos).setBlock(convertToLocalChunk(blockPos), registry.getId(block.getType().getId()));
        } else throw new IllegalChunkAccessException("Cannot modify a non-existing chunk. "+p);
    }

    public static Vector3i convertToLocalChunk(Vector3i blockPos) {
        return convertToLocalChunk(blockPos.x, blockPos.y, blockPos.z);
    }

    public Map<ChunkPos, Chunk> getChunks() {
        return Map.copyOf(chunks);
    }

    public Chunk getChunk(Vector2i pos) {
        return chunks.get(new ChunkPos(pos.x, pos.y));
    }

    public List<ChunkPos> getMissingSurroundingChunks(ChunkPos center, int radius) {
        List<ChunkPos> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos p = new ChunkPos(center.x() + dx, center.z() + dz);
                if (!chunks.containsKey(p)) result.add(p);
            }
        }
        return result;
    }

    public void addChunk(ChunkData data, ShaderProgram program, TextureAtlas atlas) {
        chunks.put(new ChunkPos(data.position.x,data.position.y),data.createChunk(program,atlas,registry));
    }

    public List<Chunk> getAllChunks() {
        return new ArrayList<>(chunks.values());
    }

    public boolean isTransparent(int x, int y, int z) {
        Block block = getBlock(x,y,z);
        return block == null;
    }

    public BlockRegistry getRegistry() {
        return registry;
    }
}