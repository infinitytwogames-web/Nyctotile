package org.infinitytwo.nyctotile.core.renderer;

import org.infinitytwo.nyctotile.core.manager.ChunkManager;
import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.manager.WorkerThreads;
import org.infinitytwo.nyctotile.block.BlockType;
import org.infinitytwo.nyctotile.core.data.ChunkData;
import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;
import org.infinitytwo.nyctotile.core.exception.IllegalChunkAccessException;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.world.GridMap;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

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
public class Chunk extends ChunkData {
    private int vaoId;
    private int vboId;
    private int vertexCount;
    private int vboCapacityFloats = 0;

    private final ShaderProgram shaderProgram;
    private volatile boolean isDirty = false;
    private final TextureAtlas atlas;
    private final BlockRegistry registry;

    public Chunk(Vector2i position, ShaderProgram shaderProgram, TextureAtlas atlas, GridMap map, BlockRegistry registry) {
        super(position);
        this.shaderProgram = shaderProgram;
        this.atlas = atlas;
        this.map = map;
        this.registry = registry;
        this.blocks = new int[SIZE_X * SIZE_Y * SIZE_Z];
        setupMeshBuffers();
    }
    
    public Chunk(Vector2i position, TextureAtlas atlas, GridMap map, BlockRegistry registry) {
        super(position);
        this.position = new Vector2i(position);
        this.shaderProgram = new ShaderProgram(
                """
                        #version 330 core
                        layout (location = 0) in vec3 aPos;
                        layout (location = 1) in vec2 aTexCoord;
                        layout (location = 2) in float aBrightness;
                        
                        out vec2 TexCoord;
                        out float Brightness;
                        
                        uniform mat4 model;
                        uniform mat4 view;
                        uniform mat4 projection;
                        
                        void main() {
                            TexCoord = aTexCoord;
                            Brightness = aBrightness;
                            gl_Position = projection * view * model * vec4(aPos, 1.0);
                        }
                        """,
                """
                        #version 330 core
                        in vec2 TexCoord;
                        in float Brightness;
                        
                        uniform sampler2D ourTexture;
                        
                        out vec4 FragColor;
                        
                        void main() {
                            vec4 texColor = texture(ourTexture, TexCoord);
                            FragColor = vec4(texColor.rgb * Brightness, texColor.a);
                        }
                        """);
        this.atlas = atlas;
        this.map = map;
        this.registry = registry;
        this.blocks = new int[SIZE_X * SIZE_Y * SIZE_Z];
        setupMeshBuffers();
    }

    private static int getIndex(int x, int y, int z) {
        return (x * SIZE_Y * SIZE_Z) + (y * SIZE_Z) + z;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    public void setBlock(int x, int y, int z, int blockId, boolean generator) throws IllegalChunkAccessException {
        if (inBounds(x, y, z)) {
            blocks[getIndex(x, y, z)]=blockId;
            isDirty = true;
            if (x == 0 || x == SIZE_X - 1 ||
                    z == 0 || z == SIZE_Z - 1) {
                if (!generator) notifyNeighboringChunks(x, y, z);
            }
        } else {
            throw new IllegalChunkAccessException("Block position out of chunk bounds");
        }
    }

    public void setBlock(int x, int y, int z, int blockId) throws IllegalChunkAccessException {
        setBlock(x, y, z, blockId, false);
    }

    private void notifyNeighboringChunks(int x, int y, int z) {
        Vector2i currentChunkPos = this.getPosition();

        if (x == 0) map.rebuildChunk(currentChunkPos.x - 1, currentChunkPos.y);
        if (x == SIZE_X - 1) map.rebuildChunk(currentChunkPos.x + 1, currentChunkPos.y);
        if (z == 0) map.rebuildChunk(currentChunkPos.x, currentChunkPos.y - 1);
        if (z == SIZE_Z - 1) map.rebuildChunk(currentChunkPos.x, currentChunkPos.y + 1);
    }

    private synchronized void setupMeshBuffers() {
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
    }

    public synchronized void buildMeshData() {
        isDirty = false;
        NFloatBuffer buffer = new NFloatBuffer();
        
        ChunkManager.run(() -> {
            for (int x = 0; x < SIZE_X; x++) {
                for (int y = 0; y < SIZE_Y; y++) {
                    for (int z = 0; z < SIZE_Z; z++) {
                        int id = getBlockId(x, y, z);
                        if (id == 0) continue;

                        BlockType type = registry.get(id);
                        if (type == null) continue;

                        type.buildModel(map, GridMap.convertToWorldPosition(position, x, y, z), atlas, registry, buffer);
                    }
                }
            }

            WorkerThreads.dispatch(() -> {
                uploadMesh(buffer);
                buffer.close();
            });
        });
    }

    private synchronized void uploadMesh(NFloatBuffer nBuffer) {
        int totalFloats = nBuffer.getWritten();
        FloatBuffer buffer = nBuffer.getBuffer();

        vertexCount = totalFloats / 6;

        if (totalFloats == 0) return; // Skip if empty

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        // --- OPTIMIZATION: Use glBufferSubData when possible ---
        if (totalFloats > vboCapacityFloats) {
            // Case 1: The new mesh is LARGER than the GPU capacity.
            // We must re-allocate on the GPU (glBufferData).
            GL15.glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            vboCapacityFloats = totalFloats; // Update GPU capacity
        } else {
            // Case 2: The new mesh FITS within the existing GPU capacity.
            // Fast update: Only copy the data, no reallocation.
            GL15.glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        }

        // The Attribute Pointers only need to be set once, but it's often safer
        // to keep them here or in setupMeshBuffers if your VAO is simple.
        // Since you are calling glBufferData/glBufferSubData, the data itself is updated,
        // and the VAO bindings are valid.

        int stride = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * Float.BYTES);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        isDirty = false;
    }

    public synchronized void draw(Camera camera, Window window) {
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

    public synchronized void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboId);
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
        shaderProgram.cleanup();
    }

    public int[] getBlockData() {
        return blocks;
    }

    public void rebuild() {
        buildMeshData();
    }

    public void dirty() {
        isDirty = true;
    }

    public void setBlock(Vector3i pos, int id, boolean generator) throws IllegalChunkAccessException {
        setBlock(pos.x, pos.y, pos.z, id, generator);
    }

    public void setBlock(Vector3i pos, int id) throws IllegalChunkAccessException {
        setBlock(pos, id, false);
    }

    public boolean isDirty() {
        return isDirty;
    }

    public static Chunk of(ChunkData data, GridMap map, ShaderProgram program, TextureAtlas atlas, BlockRegistry registry) throws IllegalChunkAccessException {
        Chunk chunk = new Chunk(data.getPosition(), program, atlas, map, registry);
        int[] sourceBlocks = data.getBlockIds();
        System.arraycopy(sourceBlocks, 0, chunk.blocks, 0, chunk.blocks.length);

        chunk.dirty();
        return chunk;
    }
    
    public static Chunk of(ChunkData data, GridMap map, TextureAtlas atlas, BlockRegistry registry) throws IllegalChunkAccessException {
        Chunk chunk = new Chunk(data.getPosition(), atlas, map, registry);
        int[] sourceBlocks = data.getBlockIds();
        System.arraycopy(sourceBlocks, 0, chunk.blocks, 0, chunk.blocks.length);
        
        chunk.dirty();
        return chunk;
    }
    
    public int getBlockId(Vector3i pos) {
        return getBlockId(pos.x, pos.y, pos.z);
    }
}
