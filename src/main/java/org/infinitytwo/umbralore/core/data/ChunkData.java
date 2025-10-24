package org.infinitytwo.umbralore.core.data;

import org.infinitytwo.umbralore.core.data.buffer.NIntBuffer;
import org.infinitytwo.umbralore.core.exception.IllegalChunkAccessException;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.renderer.Chunk;
import org.infinitytwo.umbralore.core.renderer.ShaderProgram;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.ServerGridMap;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ChunkData {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 128;
    public static final int SIZE_Z = 16;
    private transient GridMap map;
    public Vector2i position;
    private int[] blocks = new int[SIZE_X * SIZE_Y * SIZE_Z];
    private final Map<Vector3i, byte[]> blockData = new HashMap<>();

    public ChunkData(Vector2i position, GridMap map) {
        this.position = position;
        this.map = map;
    }

    public ChunkData(GridMap.ChunkPos position, GridMap map) {
        this.position = new Vector2i(position.x(),position.z());
        this.map = map;
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

    public ChunkData(Vector2i position, ServerGridMap map) {
        this.position = position;
    }

    public ChunkData(ServerGridMap.ChunkPos position) {
        this.position = new Vector2i(position.x(),position.z());
    }

    public ChunkData(Vector2i pos) {
        this.position = pos;
    }

    public static ChunkData unserialize(byte[] data, GridMap map) throws IOException {
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
        DataInputStream inStream = new DataInputStream(arrayInputStream);

        int x = inStream.readInt();
        int y = inStream.readInt();

        ChunkData chunk = new ChunkData(new Vector2i(x,y), map);
        NIntBuffer buffer = new NIntBuffer();

        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(inStream.readInt());
        }

        chunk.blocks = buffer.array();
        buffer.cleanup();

        return chunk;
    }

    public Chunk createChunk(ShaderProgram program, TextureAtlas atlas, BlockRegistry registry) {
        Chunk chunk = new Chunk(position, program, atlas, map, registry);
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
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
        return (x * SIZE_Y * SIZE_Z) + (y * SIZE_Z) + z;
    }

    private boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
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
