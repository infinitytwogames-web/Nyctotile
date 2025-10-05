package org.infinitytwo.umbralore.renderer;

import org.infinitytwo.umbralore.Window;
import org.infinitytwo.umbralore.block.BlockType;
import org.infinitytwo.umbralore.block.ServerBlockType;
import org.infinitytwo.umbralore.data.ChunkData;
import org.infinitytwo.umbralore.data.ChunkMeshData;
import org.infinitytwo.umbralore.exception.IllegalChunkAccessExecption;
import org.infinitytwo.umbralore.model.TextureAtlas;
import org.infinitytwo.umbralore.registry.BlockRegistry;
import org.infinitytwo.umbralore.world.GridMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Chunk is a batch renderer on its own. It manages block and its data into byte[] storage.
 * This class also manages rendering.
 * <br><br>
 * [ Disclaimer ]<br>
 * This class only exists client-side only. Use ChunkData class for server-side operations.
 * <br><br>
 * For nerds or expert mod makers:<br>
 * Here is the vertices list:
 * [ x (float), y (float), z (float), u (float), v (float), brightness (float 0-1) ]
 * The winding is: Clockwise (CW)
 */
public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 128;
    public static final int SIZE_Z = 16;

    private final Vector2i chunkPos;
    private final int[] blocks;
    private final Map<Vector3i, byte[]> blockData = new HashMap<>();

    private int vaoId;
    private int vboId;
    private int vertexCount;

    private final ShaderProgram shaderProgram;
    private boolean isDirty = false;
    private final TextureAtlas atlas;
    private final GridMap gridMap;
    private final BlockRegistry registry;

    public Chunk(Vector2i chunkPos, ShaderProgram shaderProgram, TextureAtlas atlas, GridMap map, BlockRegistry registry) {
        this.chunkPos = new Vector2i(chunkPos);
        this.shaderProgram = shaderProgram;
        this.atlas = atlas;
        this.gridMap = map;
        this.registry = registry;
        this.blocks = new int[SIZE_X * SIZE_Y * SIZE_Z];
        setupMeshBuffers();
    }

    private int getIndex(int x, int y, int z) {
        return (x * SIZE_Y * SIZE_Z) + (y * SIZE_Z) + z;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    public void setBlock(int x, int y, int z, int blockId, boolean generator) throws IllegalChunkAccessExecption {
        if (inBounds(x, y, z)) {
            blocks[getIndex(x, y, z)] = blockId;
            isDirty = true;
            if (x == 0 || x == SIZE_X - 1 ||
                    z == 0 || z == SIZE_Z - 1) {
                if (!generator) notifyNeighboringChunks(x, y, z);
            }
        } else {
            throw new IllegalChunkAccessExecption("Block position out of chunk bounds");
        }
    }

    public void setData(int x, int y, int z, byte[] data) throws IllegalChunkAccessExecption {
        if (inBounds(x,y,z)) blockData.replace(new Vector3i(x,y,z), data);
        else throw new IllegalChunkAccessExecption("Position ("+x+", "+y+", "+z+") is out of bounds");
    }

    public void setData(Vector3i pos, byte[] data) throws IllegalChunkAccessExecption {
        if (inBounds(pos.x,pos.y,pos.z)) blockData.replace(new Vector3i(pos),data);
        else throw new IllegalChunkAccessExecption("Position ("+pos.x+", "+pos.y+", "+pos.z+") is out of bounds");
    }

    public byte[] getData(int x, int y, int z) throws IllegalChunkAccessExecption {
        if (inBounds(x,y,z)) return blockData.get(new Vector3i(x,y,z));
        else throw new IllegalChunkAccessExecption("Position ("+x+", "+y+", "+z+") is out of bounds");
    }

    public byte[] getData(Vector3i pos) throws IllegalChunkAccessExecption {
        if (inBounds(pos.x,pos.y,pos.z)) return blockData.get(pos);
        else throw new IllegalChunkAccessExecption("Position ("+pos.x+", "+pos.y+", "+pos.z+") is out of bounds");
    }

    public void setBlock(int x, int y, int z, int blockId) throws IllegalChunkAccessExecption {
        setBlock(x,y,z,blockId,false);
    }

    private void notifyNeighboringChunks(int x, int y, int z) {
        Vector2i currentChunkPos = this.getPosition();

        if (x == 0) gridMap.rebuildChunk(currentChunkPos.x - 1, currentChunkPos.y);
        if (x == SIZE_X - 1) gridMap.rebuildChunk(currentChunkPos.x + 1, currentChunkPos.y);
        if (z == 0) gridMap.rebuildChunk(currentChunkPos.x, currentChunkPos.y - 1);
        if (z == SIZE_Z - 1) gridMap.rebuildChunk(currentChunkPos.x, currentChunkPos.y + 1);
    }

    public int getBlockId(int x, int y, int z) {
        return inBounds(x, y, z) ? blocks[getIndex(x, y, z)] : 0;
    }

    private void setupMeshBuffers() {
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
    }

    public ChunkData toChunkData() {
        return new ChunkData(chunkPos,gridMap);
    }

    public ChunkMeshData buildMeshData() {
        ArrayList<Float> combinedVertices = new ArrayList<>();
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    int id = getBlockId(x, y, z);
                    if (id == 0) continue;

                    BlockType type = registry.get(id);
                    if (type == null) continue;
                    Vector3i worldPos = new Vector3i(chunkPos.x * SIZE_X + x, y, chunkPos.y * SIZE_Z + z);

                    int before = combinedVertices.size();
                    if (type instanceof ServerBlockType) throw new RuntimeException("ServerBlockType is not supported!");
                    type.buildModel(gridMap, worldPos,atlas, registry, combinedVertices);
                    int added = combinedVertices.size() - before;
                }
            }
        }

        return new ChunkMeshData(combinedVertices);
    }

    private void uploadMesh(ChunkMeshData meshData) {
        vertexCount = meshData.getVertexCount();
        FloatBuffer buffer = meshData.toFloatBuffer();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        int stride = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * Float.BYTES);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        meshData.free();

        isDirty = false;
    }

    public void draw(Camera camera, Window window) {
        if (isDirty) rebuild();
        if (vertexCount == 0) return;

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glFrontFace(GL_CW);

        shaderProgram.bind();

        Matrix4f model = new Matrix4f().identity();
        Matrix4f view = camera.getViewMatrix();
        Vector2i windowSize = window.getSize();
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(camera.getFov()), (float) windowSize.x / windowSize.y, 0.1f, 1024f);

        shaderProgram.setUniformMatrix4fv("model", model);
        shaderProgram.setUniformMatrix4fv("view", view);
        shaderProgram.setUniformMatrix4fv("projection", projection);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        atlas.getTexture().bind();
        shaderProgram.setUniform1i("ourTexture", 0);

        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
        shaderProgram.unbind();
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboId);
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);

        if (this.meshData != null) {
            this.meshData.free();
        }
    }

    public Vector2i getPosition() {
        return new Vector2i(chunkPos);
    }

    public int[] getBlockData() {
        return blocks;
    }

    // Add a field to store the ChunkMeshData object
    private ChunkMeshData meshData;

    public void rebuild() {
        // If a mesh already exists, free its memory before creating a new one
        if (this.meshData != null) {
            this.meshData.free();
        }

        // Create and build the new mesh data
        this.meshData = buildMeshData();
        if (this.meshData.getVertexCount() > 0) {
            this.meshData.build();
            uploadMesh(this.meshData);
        }

        isDirty = false;
    }

    public void dirty() {
        isDirty = true;
    }

    public void setBlock(Vector3i pos, int id, boolean generator) throws IllegalChunkAccessExecption {
        setBlock(pos.x, pos.y, pos.z, id, generator);
    }

    public void setBlock(Vector3i pos, int id) throws IllegalChunkAccessExecption {
        setBlock(pos, id, false);
    }

    public boolean isDirty() {
        return isDirty;
    }

    public static Chunk of(ChunkData data, GridMap map, ShaderProgram program, TextureAtlas atlas, BlockRegistry registry) throws IllegalChunkAccessExecption {
        Chunk chunk = new Chunk(data.position,program,atlas,map, registry);
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    int id = data.getBlockId(x, y, z);
                    if (id != 0) { // assuming 0 = air or empty
                        chunk.setBlock(x, y, z, id);
                    }
                }
            }
        }
        return chunk;
    }

    public int getBlockId(Vector3i pos) {
        return getBlockId(pos.x,pos.y,pos.z);
    }
}
