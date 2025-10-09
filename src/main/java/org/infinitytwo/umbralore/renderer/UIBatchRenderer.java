package org.infinitytwo.umbralore.renderer;

import org.infinitytwo.umbralore.Main;
import org.infinitytwo.umbralore.RGBA;
import org.infinitytwo.umbralore.constants.ShaderFiles;
import org.infinitytwo.umbralore.event.SubscribeEvent;
import org.infinitytwo.umbralore.event.bus.EventBus;
import org.infinitytwo.umbralore.event.state.WindowResizedEvent;
import org.infinitytwo.umbralore.model.TextureAtlas;
import org.infinitytwo.umbralore.ui.UI;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.infinitytwo.umbralore.constants.Constants.UI_DESIGN_HEIGHT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL11.glBindTexture;

public class UIBatchRenderer {
    private int vaoId, vboId;
    private FloatBuffer vertexBuffer;
    private float[] vertexData;
    private int vertexDataIndex = 0;
    private final int MAX_QUADS = 1000;
    private final int VERTEX_SIZE = 8;
    private final int VERTICES_PER_QUAD = 6;
    private final int FLOATS_PER_QUAD = VERTEX_SIZE * VERTICES_PER_QUAD;

    private final Matrix4f projection = new Matrix4f();
    private int shaderProgramId;

    // --- BATCH STATE MANAGEMENT (NEW) ---
    private boolean currentBatchIsTextured = false;
    private int currentAtlasTextureID = 0; // The texture ID of the currently bound atlas

    public UIBatchRenderer(int shaderProgramId) {
        this.shaderProgramId = shaderProgramId;
        init();
    }

    public UIBatchRenderer(ShaderProgram program) {
        this.shaderProgramId = program.getProgramId();
        init();
    }

    public UIBatchRenderer() {
        this.shaderProgramId = new ShaderProgram(ShaderFiles.uiVertex, ShaderFiles.uiFragment).getProgramId();
        init();
    }

    private void init() {
        EventBus.register(this);

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        vertexData = new float[MAX_QUADS * FLOATS_PER_QUAD];
        vertexBuffer = MemoryUtil.memAllocFloat(MAX_QUADS * FLOATS_PER_QUAD);

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) vertexData.length * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        // Attribute Pointers remain correct
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 0);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 2 * Float.BYTES);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 6 * Float.BYTES);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // Set the u_texture sampler uniform once
        GL20.glUseProgram(shaderProgramId);
        int locSampler = glGetUniformLocation(shaderProgramId, "u_texture");
        glUniform1i(locSampler, 0); // Tell shader u_texture always refers to Texture Unit 0
        GL20.glUseProgram(0);

        onWindowResize(new WindowResizedEvent(1024, 512, Main.getWindow()));
    }

    public void queue(UI ui) {
        // --- BATCH BREAK: Switching from Textured to Untextured ---
        if (currentBatchIsTextured) {
            flush();
            currentBatchIsTextured = false;
            currentAtlasTextureID = 0;
            begin();
        }

        if (vertexDataIndex + FLOATS_PER_QUAD > vertexData.length) {
            flush();
            begin();
        }

        RGBA color = ui.getBackgroundColor();
        int x = ui.getPosition().x();
        int y = ui.getPosition().y();
        float w = ui.getWidth();
        float h = ui.getHeight();

        // For untextured quads, UVs are irrelevant but must exist. We set them to default (0,0) to (1,1).
        Vector2f texCoordTopLeft = new Vector2f(0.0f, 0.0f);
        Vector2f texCoordBottomRight = new Vector2f(1.0f, 1.0f);
        Vector2f texCoordTopRight = new Vector2f(1.0f, 0.0f);
        Vector2f texCoordBottomLeft = new Vector2f(0.0f, 1.0f);

        float[] quad = {
                // First triangle: Top-Left, Bottom-Left, Bottom-Right
                // X, Y, R, G, B, A, U, V
                x, y, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordTopLeft.x, texCoordTopLeft.y,
                x, y + h, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordBottomLeft.x, texCoordBottomLeft.y,
                x + w, y + h, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordBottomRight.x, texCoordBottomRight.y,

                // Second triangle: Top-Left, Bottom-Right, Top-Right
                // X, Y, R, G, B, A, U, V
                x, y, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordTopLeft.x, texCoordTopLeft.y,
                x + w, y + h, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordBottomRight.x, texCoordBottomRight.y,
                x + w, y, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordTopRight.x, texCoordTopRight.y
        };

        System.arraycopy(quad, 0, vertexData, vertexDataIndex, quad.length);
        vertexDataIndex += quad.length;
    }

    public void queueTextured(int x, int y, float w, float h, int textureIndex, TextureAtlas atlas, RGBA color) {

        // --- BATCH BREAK: Switching texture OR switching from untextured ---
        int atlasID = atlas.getTexture().getTextureID();

        if (!currentBatchIsTextured || currentAtlasTextureID != atlasID) {
            flush();
            currentBatchIsTextured = true;
            currentAtlasTextureID = atlasID;
            begin();
        }

        if (vertexDataIndex + FLOATS_PER_QUAD > vertexData.length) {
            flush();
            begin();
        }

        // NOTE: The binding is now done in flush(), not here.

        // Get UVs from atlas
        float[] uv = atlas.getUVCoords(textureIndex);
        float u0 = uv[0];
        float v0 = uv[1];
        float u1 = uv[2];
        float v1 = uv[3];

        float[] quad = {
                // First triangle
                x, y, color.r(), color.g(), color.b(), color.a(), u0, v0,
                x, y + h, color.r(), color.g(), color.b(), color.a(), u0, v1,
                x + w, y + h, color.r(), color.g(), color.b(), color.a(), u1, v1,

                // Second triangle
                x, y, color.r(), color.g(), color.b(), color.a(), u0, v0,
                x + w, y + h, color.r(), color.g(), color.b(), color.a(), u1, v1,
                x + w, y, color.r(), color.g(), color.b(), color.a(), u1, v0
        };

        System.arraycopy(quad, 0, vertexData, vertexDataIndex, quad.length);
        vertexDataIndex += quad.length;
    }

    public void begin() {
        vertexDataIndex = 0; // Reset index for new batch
    }

    public void flush() {
        if (vertexDataIndex == 0) return; // Nothing to draw

        // 1. Prepare and upload vertex data
        vertexBuffer.clear();
        vertexBuffer.put(vertexData, 0, vertexDataIndex).flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        // 2. Activate Shader and VAO
        GL30.glBindVertexArray(vaoId);
        GL20.glUseProgram(shaderProgramId);

        // 3. Set Uniforms based on current batch state

        // --- CRITICAL: Set the useTexture flag ---
        int locUseTexture = glGetUniformLocation(shaderProgramId, "useTexture");
        // Convert Java boolean to int (1 for true, 0 for false)
        glUniform1i(locUseTexture, currentBatchIsTextured ? 1 : 0);

        // Set projection matrix
        int locProj = glGetUniformLocation(shaderProgramId, "projection");
        try (var stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            projection.get(fb);
            glUniformMatrix4fv(locProj, false, fb);
        }

        // --- CRITICAL: Bind Texture for Textured Batch ---
        if (currentBatchIsTextured) {
            // Since we set u_texture to 0 in init(), we bind to GL_TEXTURE0
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, currentAtlasTextureID);
        } else {
            // Unbind texture unit 0 (good practice)
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        // 4. OpenGL State Setup
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // 5. Draw
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexDataIndex / VERTEX_SIZE);

        // 6. Cleanup OpenGL State
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0);
        GL30.glBindVertexArray(0);
        // No need to unbind texture here if we unbound it above,
        // but leaving glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, 0); in the else block is cleaner.
    }

    public void changeProgramId(int id) {
        shaderProgramId = id;
    }

    public int getMaxQuads() {
        return MAX_QUADS;
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL30.glDeleteVertexArrays(vaoId);
        MemoryUtil.memFree(vertexBuffer);
    }

    @SubscribeEvent
    public void onWindowResize(WindowResizedEvent e) {
        float currentWindowWidth = e.width;
        float currentWindowHeight = e.height;

        float currentVirtualWidth = (currentWindowWidth / currentWindowHeight) * UI_DESIGN_HEIGHT;

        // Set the orthographic projection matrix
        projection.setOrtho(
                0.0f,
                currentVirtualWidth,
                UI_DESIGN_HEIGHT,
                0.0f,
                -100.0f,
                100.0f
        );
    }
}
