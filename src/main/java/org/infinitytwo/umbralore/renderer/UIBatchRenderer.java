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
import org.joml.Vector2f; // Added for texture coordinates
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.infinitytwo.umbralore.constants.Constants.UI_DESIGN_HEIGHT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUniform1i; // For boolean uniform

public class UIBatchRenderer {
    private int vaoId, vboId;
    private FloatBuffer vertexBuffer;
    private float[] vertexData;
    private int vertexDataIndex = 0;
    private final int MAX_QUADS = 1000;
    // CORRECTED: x, y, r, g, b, a, u, v (8 floats per vertex)
    private final int VERTEX_SIZE = 8;
    private final int VERTICES_PER_QUAD = 6; // Two triangles make a quad
    private final int FLOATS_PER_QUAD = VERTEX_SIZE * VERTICES_PER_QUAD; // 8 * 6 = 48 floats per quad

    // Initial projection matrix setup (will be updated by onWindowResize)
    private final Matrix4f projection = new Matrix4f();

    private int shaderProgramId;

    public UIBatchRenderer(int shaderProgramId) {
        this.shaderProgramId = shaderProgramId;
        init();
    }

    public UIBatchRenderer(ShaderProgram program) {
        this.shaderProgramId = program.getProgramId();
        init();
    }

    public UIBatchRenderer() {
        // Ensure Display.height is initialized before this constructor is called
        // Or handle initial resize explicitly after Display is ready.
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
        // Allocate buffer on GPU, DYNAMIC_DRAW for frequent updates
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) vertexData.length * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        // --- Vertex Attribute Pointers (CRITICAL CORRECTION) ---
        // Position (location 0): 2 floats (x, y)
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 0);
        // Color (location 1): 4 floats (r, g, b, a)
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 2 * Float.BYTES);
        // Texture Coordinates (location 2): 2 floats (u, v)
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 6 * Float.BYTES); // Offset: 2 (pos) + 4 (color) = 6 floats

        // Enable all attribute arrays that the shader expects
        GL20.glEnableVertexAttribArray(0); // aPos
        GL20.glEnableVertexAttribArray(1); // aColor
        GL20.glEnableVertexAttribArray(2); // aTexturePos (NEWLY ENABLED)

        GL30.glBindVertexArray(0); // Unbind VAO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); // Unbind VBO

        onWindowResize(new WindowResizedEvent(1024,512, Main.getWindow()));
    }

    public void queue(UI ui) {
        // Check if current batch is full, if so, flush and start new
        if (vertexDataIndex + FLOATS_PER_QUAD > vertexData.length) {
            flush();
            begin(); // Reset index for new batch
        }

        RGBA color = ui.getBackgroundColor();
        int x = ui.getPosition().x();
        int y = ui.getPosition().y();
        float w = ui.getWidth();
        float h = ui.getHeight();

        Vector2f texCoordTopLeft = new Vector2f(0.0f, 0.0f);
        Vector2f texCoordBottomRight = new Vector2f(1.0f, 1.0f);
        Vector2f texCoordTopRight = new Vector2f(1.0f, 0.0f);
        Vector2f texCoordBottomLeft = new Vector2f(0.0f, 1.0f);

        // If a texture is present, you might want to get specific UVs from it
        // For a simple quad, (0,0) to (1,1) is standard.
        // If you have a texture atlas, you'd calculate these based on the sprite's location in the atlas.
        // For now, these default values work for any untextured quad.

        // CORRECTED: Quad vertices for two triangles (GL_TRIANGLES)
        // Assuming (0,0) is top-left, Y increases downwards
        float[] quad = {
                // First triangle: Top-Left, Bottom-Left, Bottom-Right
                // X, Y, R, G, B, A, U, V
                x,     y,     color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordTopLeft.x, texCoordTopLeft.y, // Top-Left
                x,     y + h, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordBottomLeft.x, texCoordBottomLeft.y, // Bottom-Left
                x + w, y + h, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordBottomRight.x, texCoordBottomRight.y, // Bottom-Right

                // Second triangle: Top-Left, Bottom-Right, Top-Right
                // X, Y, R, G, B, A, U, V
                x,     y,     color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordTopLeft.x, texCoordTopLeft.y, // Top-Left
                x + w, y + h, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordBottomRight.x, texCoordBottomRight.y, // Bottom-Right
                x + w, y,     color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), texCoordTopRight.x, texCoordTopRight.y  // Top-Right
        };

        System.arraycopy(quad, 0, vertexData, vertexDataIndex, quad.length);
        vertexDataIndex += quad.length;
    }

    public void queueTextured(int x, int y, float w, float h, int textureIndex, TextureAtlas atlas, RGBA color) {
        if (vertexDataIndex + FLOATS_PER_QUAD > vertexData.length) {
            flush();
            begin();
        }

        // Bind the atlas before writing vertices
        atlas.getTexture().bind();

        // Get UVs from atlas
        float[] uv = atlas.getUVCoords(textureIndex);
        float u0 = uv[0];
        float v0 = uv[1];
        float u1 = uv[2];
        float v1 = uv[3];

        float[] quad = {
                // First triangle
                x,     y,     color.r(), color.g(), color.b(), color.a(), u0, v0,
                x,     y + h, color.r(), color.g(), color.b(), color.a(), u0, v1,
                x + w, y + h, color.r(), color.g(), color.b(), color.a(), u1, v1,

                // Second triangle
                x,     y,     color.r(), color.g(), color.b(), color.a(), u0, v0,
                x + w, y + h, color.r(), color.g(), color.b(), color.a(), u1, v1,
                x + w, y,     color.r(), color.g(), color.b(), color.a(), u1, v0
        };

        System.arraycopy(quad, 0, vertexData, vertexDataIndex, quad.length);
        vertexDataIndex += quad.length;

        // Tell shader we are using a texture
        int locUseTexture = glGetUniformLocation(shaderProgramId, "useTexture");
        glUniform1i(locUseTexture, 1);
    }

    public void begin() {
        vertexDataIndex = 0; // Reset index for new batch
    }

    public void flush() {
        if (vertexDataIndex == 0) return; // Nothing to draw

        // Prepare vertex buffer for upload
        vertexBuffer.clear();
        vertexBuffer.put(vertexData, 0, vertexDataIndex).flip();

        // Bind VBO and upload data
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        // Use glBufferSubData to update only the modified portion of the buffer
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        // Bind VAO and use shader program
        GL30.glBindVertexArray(vaoId);
        GL20.glUseProgram(shaderProgramId);

        // --- Set Uniforms (CRITICAL CORRECTION) ---
        // 1. Set projection matrix uniform (name corrected to "projection")
        int locProj = glGetUniformLocation(shaderProgramId, "projection"); // Corrected uniform name
        try (var stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            projection.get(fb); // Get matrix data into FloatBuffer
            glUniformMatrix4fv(locProj, false, fb); // Upload to shader
        }

        // 2. Set useTexture uniform (NEW)
        int locUseTexture = glGetUniformLocation(shaderProgramId, "useTexture");
        glUniform1i(locUseTexture, 0); // 0 for false (no texture for now, as per your UI element)
        // If you implement texture support, you'd set this based on UI element's texture presence.

        // --- OpenGL State Setup ---
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST); // Disable depth test for 2D UI to ensure drawing order

        // Draw the batched quads
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexDataIndex / VERTEX_SIZE);

        // --- Cleanup OpenGL State ---
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0); // Unuse shader program
        GL30.glBindVertexArray(0); // Unbind VAO
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
        MemoryUtil.memFree(vertexBuffer); // Free direct buffer memory
    }

    @SubscribeEvent
    public void onWindowResize(WindowResizedEvent e) {
        // Update current window dimensions (important for the projection calculation)
        // You'll need to store these in your Display class or pass them around
        // Assuming Display.width and Display.height are updated before this event fires.
        float currentWindowWidth = e.width;
        float currentWindowHeight = e.height;

        float currentVirtualWidth = (currentWindowWidth / currentWindowHeight) * UI_DESIGN_HEIGHT;
//        System.out.println(currentVirtualWidth);

        // Set the orthographic projection matrix
        // (0,0) top-left, Y increases downwards, mapping to (currentVirtualWidth, UI_DESIGN_HEIGHT)
        projection.setOrtho(
                0.0f,               // Left
                currentVirtualWidth, // Right
                UI_DESIGN_HEIGHT,   // Bottom (max Y)
                0.0f,               // Top (min Y)
                -100.0f,              // Near
                100.0f                // Far
        );
    }
}