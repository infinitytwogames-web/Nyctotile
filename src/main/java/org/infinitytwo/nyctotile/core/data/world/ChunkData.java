package org.infinitytwo.nyctotile.core.data.world;

import org.infinitytwo.nyctotile.core.data.buffer.NIntBuffer;
import org.infinitytwo.nyctotile.core.exception.IllegalChunkAccessException;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.renderer.Chunk;
import org.infinitytwo.nyctotile.core.renderer.ShaderProgram;
import org.infinitytwo.nyctotile.core.world.GridMap;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ChunkData {
    public static final int SIZE = 16;
    public static final int SIZE_Y = 128;
    public static final int SIZE_X = SIZE;
    public static final int SIZE_Z = SIZE;
    protected transient GridMap map;
    protected Vector2i position;
    protected int[] blocks = new int[SIZE * SIZE_Y * SIZE];
    private final Map<Vector3i, byte[]> blockData = new HashMap<>();
    
    public ChunkData(ChunkPos chunkPos) {
        this.position = new Vector2i();
        position.set(chunkPos.x(),chunkPos.z());
    }
    
    public static ChunkData of(byte[] data) throws IOException {
        return unserialize(data);
    }
    
    public static ChunkData of(int chunkX, int chunkZ, int[] blocks) {
        ChunkData data = new ChunkData(new Vector2i(chunkX,chunkZ));
        data.blocks = blocks;
        return data;
    }
    
    public void setData(int x, int y, int z, byte[] data) throws IllegalChunkAccessException {
        if (isInBounds(x,y,z)) blockData.replace(new Vector3i(x,y,z), data);
        else throw new IllegalChunkAccessException("Position ("+x+", "+y+", "+z+") is out of bounds");
    }

    public void setData(Vector3i pos, byte[] data) throws IllegalChunkAccessException {
        if (isInBounds(pos.x,pos.y,pos.z)) blockData.replace(new Vector3i(pos),data);
        else throw new IllegalChunkAccessException("Position ("+pos.x+", "+pos.y+", "+pos.z+") is out of bounds");
    }

    public byte[] getData(int x, int y, int z) throws IllegalChunkAccessException {
        if (isInBounds(x,y,z)) return blockData.getOrDefault(new Vector3i(x,y,z), new byte[0]);
        else throw new IllegalChunkAccessException("Position ("+x+", "+y+", "+z+") is out of bounds");
    }

    public byte[] getData(Vector3i pos) throws IllegalChunkAccessException {
        if (isInBounds(pos.x,pos.y,pos.z)) return blockData.getOrDefault(pos, new byte[0]);
        else throw new IllegalChunkAccessException("Position ("+pos.x+", "+pos.y+", "+pos.z+") is out of bounds");
    }

    public ChunkData(Vector2i pos) {
        this.position = pos;
    }

    public static ChunkData unserialize(byte[] data) throws IOException {
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
        DataInputStream inStream = new DataInputStream(arrayInputStream);

        int x = inStream.readInt();
        int y = inStream.readInt();

        ChunkData chunk = new ChunkData(new Vector2i(x,y));
        NIntBuffer buffer = new NIntBuffer();

        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(inStream.readInt());
        }

        chunk.blocks = buffer.array();
        buffer.cleanup();

        return chunk;
    }

    public static ChunkData unserialize(ByteBuffer data) {
        int x = data.getInt(), y = data.getInt();

        ChunkData chunk = new ChunkData(new Vector2i(x,y));
        NIntBuffer buffer = new NIntBuffer();

        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(data.getInt());
        }

        chunk.blocks = buffer.array();
        buffer.cleanup();

        return chunk;
    }

    public Chunk createChunk(ShaderProgram program, TextureAtlas atlas, BlockRegistry registry) {
        Chunk chunk = new Chunk(position, program, atlas, map, registry);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE; z++) {
                    int id = blocks[getIndex(x, y, z)];
                    if (id != 0) { // assuming 0 = air or empty
                        try {
                            chunk.setBlock(x, y, z, id,false);
                        } catch (IllegalChunkAccessException ignored) {

                        }
                    }
                }
            }
        }
        chunk.dirty();
        return chunk;
    }

    public void setBlock(int x, int y, int z, int blockId) throws IllegalChunkAccessException {
//        map.getRegistry().get(blockId);
        if (isInBounds(x, y, z)) {
            blocks[getIndex(x, y, z)]=blockId;
        } else {
            throw new IllegalChunkAccessException("Block position out of chunk bounds ("+x+", "+y+", "+z+")");
        }
    }

    private int getIndex(int x, int y, int z) {
        return (x * SIZE_Y * SIZE) + (y * SIZE) + z;
    }

    private boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE;
    }

    public int getBlockId(int x, int y, int z) {
        return isInBounds(x, y, z) ? blocks[getIndex(x, y, z)] : 0;
    }

    public void setBlock(Vector3i pos, int id) throws IllegalChunkAccessException {
        setBlock(pos.x,pos.y,pos.z,id);
    }

    public Vector2i getPosition() {
        return position;
    }

    public int[] getBlockIds() {
        return blocks;
    }

    public void modify(int[] blocks) {
        System.arraycopy(blocks, 0, this.blocks, 0, blocks.length);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate((Integer.BYTES * (2 + blocks.length)));
        buffer.putInt(position.x);
        buffer.putInt(position.y);

        for (int b : blocks) {
            buffer.putInt(b);
        }

        return buffer.array();
    }
}
