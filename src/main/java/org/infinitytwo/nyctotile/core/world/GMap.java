package org.infinitytwo.nyctotile.core.world;

import org.infinitytwo.nyctotile.block.BlockType;
import org.infinitytwo.nyctotile.core.data.Block;
import org.infinitytwo.nyctotile.core.data.ChunkPos;
import org.infinitytwo.nyctotile.core.data.RaycastResult;
import org.infinitytwo.nyctotile.core.data.io.BlockDataReader;
import org.infinitytwo.nyctotile.core.exception.IllegalChunkAccessException;
import org.infinitytwo.nyctotile.core.exception.IllegalDataTypeException;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

import static org.infinitytwo.nyctotile.core.data.ChunkData.SIZE_X;
import static org.infinitytwo.nyctotile.core.data.ChunkData.SIZE_Z;

public abstract class GMap {
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
    
    public static ChunkPos worldToChunkPos(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, SIZE_Z);
        return new ChunkPos(chunkX, chunkZ);
    }
    
    public static Vector3i convertToWorldPosition(Vector2i chunk, Vector3i localChunkPos) {
        return localChunkPos.add((chunk.x * SIZE_X),0,(chunk.y * SIZE_Z));
    }
    
    protected static Vector3i convertToLocalChunk(Vector3i blockPos) {
        return convertToLocalChunk(blockPos.x, blockPos.y, blockPos.z);
    }
    
    public abstract Block getBlock(int x, int y, int z);
    public abstract void removeBlock(int x, int y, int z) throws IllegalChunkAccessException;
    public abstract void removeBlock(Vector3i pos) throws IllegalChunkAccessException;
    public abstract List<ChunkPos> getSurroundingChunks(ChunkPos center, int radius);
    public abstract void insertData(Vector3i pos, byte[] data) throws IllegalChunkAccessException;
    public abstract byte[] getData(Vector3i pos) throws IllegalChunkAccessException;
    public abstract Object getData(Vector3i pos, BlockDataReader reader, String name) throws IllegalChunkAccessException, IllegalDataTypeException;
    public abstract BlockType getBlockType(Vector3i pos);
    public abstract RaycastResult raycast(Vector3f origin, Vector3f direction, float maxDistance);
    public abstract void setBlock(Block block) throws IllegalChunkAccessException;
    public abstract boolean isBlockLoaded(int x, int y, int z);
    public abstract Block getTopBlock(int x, int z);
}
