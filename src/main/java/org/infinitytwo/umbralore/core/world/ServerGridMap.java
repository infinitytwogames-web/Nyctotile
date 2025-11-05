package org.infinitytwo.umbralore.core.world;

import org.infinitytwo.umbralore.core.data.Block;
import org.infinitytwo.umbralore.block.BlockType;
import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.data.RaycastResult;
import org.infinitytwo.umbralore.core.exception.IllegalChunkAccessException;
import org.infinitytwo.umbralore.core.exception.IllegalDataTypeException;
import org.infinitytwo.umbralore.core.data.io.BlockDataReader;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGridMap extends GMap {
    protected final ConcurrentHashMap<ChunkPos, ChunkData> chunks = new ConcurrentHashMap<>();
    protected boolean isReady = false;
    protected final BlockRegistry registry;

    public ServerGridMap(BlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Vector2i p = convertToChunkPosition(x,z);
        ChunkPos pos = new ChunkPos(p.x, p.y);

        if (chunks.containsKey(pos)) {
            int id = chunks.get(pos).getBlockId(x,y,z);
            if (id != 0) return new Block(registry.get(id));
        } else return null;
        return null;
    }

    @Override
    public void removeBlock(int x, int y, int z) throws IllegalChunkAccessException {
        Vector2i p = convertToChunkPosition(x,z);
        ChunkPos pos = new ChunkPos(p.x, p.y);

        if (chunks.containsKey(pos)) {
            chunks.get(pos).setBlock(convertToLocalChunk(x,y,z), (short) 0);
        } else throw new IllegalChunkAccessException("Cannot modify a non-existing chunk. "+p);
    }

    @Override
    public void removeBlock(Vector3i pos) throws IllegalChunkAccessException {
        removeBlock(pos.x,pos.y, pos.z);
    }

    public void addChunk(ChunkData chunk) {
        chunks.put(new ChunkPos(chunk.getPosition().x,chunk.getPosition().y),chunk);
    }

    @Override
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
    public void insertData(Vector3i pos, byte[] data) throws IllegalChunkAccessException {
        chunks.get(worldToChunkPos(pos.x, pos.z)).setData(pos, data);
    }

    @Override
    public byte[] getData(Vector3i pos) throws IllegalChunkAccessException {
        return chunks.get(worldToChunkPos(pos.x, pos.z)).getData(pos);
    }

    @Override
    public Object getData(Vector3i pos, BlockDataReader reader, String name) throws IllegalChunkAccessException, IllegalDataTypeException {
        ChunkData chunk = chunks.get(worldToChunkPos(pos.x, pos.z));
        return reader.getData(chunk.getBlockId(pos.x,pos.y,pos.z), chunk.getData(pos), name);
    }

    @Override
    public BlockType getBlockType(Vector3i pos) {
        return getBlock(pos.x,pos.y,pos.z).getType();
    }
    
    @Override
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
        Vector3i blockPos = block.getPosition();
        Vector2i p = convertToChunkPosition(blockPos);
        ChunkPos pos = new ChunkPos(p.x, p.y);

        if (chunks.containsKey(pos)) {
            chunks.get(pos).setBlock(convertToLocalChunk(blockPos), registry.getId(block.getType().getId()));
        } else throw new IllegalChunkAccessException("Cannot modify a non-existing chunk. "+p);
    }
    
    public ChunkData getChunk(Vector2i pos) throws IllegalChunkAccessException {
        ChunkData data = chunks.get(new ChunkPos(pos.x, pos.y));
        if (data == null) throw new IllegalChunkAccessException("Cannot access a non-existing chunk at (" + pos.x +", "+pos.y+")");
        return data;
    }

    public Collection<ChunkData> getChunks() {
        return Collections.unmodifiableCollection(chunks.values());
    }
}
