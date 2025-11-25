package org.infinitytwo.nyctotile.core.renderer;

import org.infinitytwo.nyctotile.core.RGB;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.constants.ShaderFiles;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.infinitytwo.nyctotile.core.Display.projection;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * Shader-based FontRenderer using dynamic VBO/VAO. No fixed-function matrices.
 * Pass the compiled shader program ID in the constructor.
 */
public class FontRenderer {
    private static final int BITMAP_W   = 1024;
    private static final int BITMAP_H   = 1024;
    private static final int FIRST_CHAR = 32;
    private static final int CHAR_COUNT = 224;
    
    private static final int[] CODEPOINTS;
    private static final int NUM_CODEPOINTS;

    private int texID;
    private STBTTBakedChar.Buffer charData;

    // Shader and uniforms
    private int locProj;
    private int locTextColor;
    private int locFontAtlas;
    private int locModel;

    // VAO/VBO for vertex data (x,y,s,t)
    private int vaoId;
    private int vboId;
    private final String fontPath;

    private float fontHeight;
    private final ShaderProgram program;
    private boolean initialized = false;
    private final Logger logger = LoggerFactory.getLogger(FontRenderer.class);

    static {
        // Generate ASCII 32 to 126 (95 characters)
        int count = 127 - 32;
        // Add 1 for the Em Dash (8212)
        CODEPOINTS = new int[count + 1];
        for (int i = 0; i < count; i++) {
            CODEPOINTS[i] = 32 + i;
        }
        // Add the Em Dash (U+2014) at the end
        CODEPOINTS[count] = 8212;
        NUM_CODEPOINTS = CODEPOINTS.length;
    }
    
    /**
     * @param fontPath    path to a TTF file
     * @param height      pixel height
     */
    public FontRenderer(String fontPath, float height) {
        this.fontHeight = height * 2;
        program = new ShaderProgram(ShaderFiles.textVertex,ShaderFiles.textFragment);
        this.fontPath = fontPath;
        EventBus.connect(this);
        init();
    }

    private void init() {
        // Query uniforms once
        locProj      = glGetUniformLocation(program.getProgramId(), "uProj");
        locTextColor = glGetUniformLocation(program.getProgramId(), "uTextColor");
        locFontAtlas = glGetUniformLocation(program.getProgramId(), "uFontAtlas");
        locModel     = program.getUniformLocation("uModel");

        // Load font data
        ByteBuffer fontBuffer;
        try {
            fontBuffer = loadFont(fontPath); // Buffer is loaded, and flipped (position=0, limit=size)
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + fontPath, e);
        }
        
        // CRITICAL FIX: Reset the buffer's position to the beginning (0)
        // before calling the baking function, ensuring it reads from the start.
        fontBuffer.position(0);
        
        // Bake glyphs
        // Ensure the size of the memory buffer matches the intended range exactly
        charData = STBTTBakedChar.malloc(NUM_CODEPOINTS);
        ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
        
        // 2. Prepare the array of code points.
        IntBuffer codePointsBuffer = BufferUtils.createIntBuffer(NUM_CODEPOINTS);
        codePointsBuffer.put(CODEPOINTS).flip();
        
        // CRITICAL: Check the return value of stbtt_BakeFontBitmap.
        // A non-zero return value indicates how many more characters *could* fit,
        // but a negative value indicates a failure or insufficient buffer.
        int result = stbtt_BakeFontBitmap(fontBuffer, fontHeight, bitmap, BITMAP_W, BITMAP_H, FIRST_CHAR, charData);
        
        // Add logging to see if the baking failed
        if (result <= 0) {
            logger.error("STBTT Bake failed or returned zero chars fit! Result: {}", result);
        }
        
        // Upload texture atlas
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, BITMAP_W, BITMAP_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Setup VAO/VBO
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, 2048 * 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        initialized = true;
    }

    public void renderText(Matrix4f projView, String text, float x, float y, int z, float r, float g, float b, float angle) {
        if (!initialized) {
            logger.error("FontRenderer is not initialized",new IllegalStateException("FontRenderer is not initialized"));
            return;
        }
        float safeZOffset = (float)z * 0.001f; // e.g., 2 * 0.001 = 0.002
        
        program.bind();
        Matrix4f model = new Matrix4f()
                .translate(x, y, safeZOffset) // Use the scaled float offset
                .rotateZ((float)Math.toRadians(angle));
        
        // Upload projection
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mat = stack.mallocFloat(16);
            FloatBuffer modelMat = stack.mallocFloat(16);
            
            projView.get(mat);
            model.get(modelMat);
            
            glUniformMatrix4fv(locProj, false, mat);
            glUniformMatrix4fv(locModel, false, modelMat);
        }
        // Color & sampler
        glUniform3f(locTextColor, r, g, b);
        glUniform1i(locFontAtlas, 0);

        // Bind atlas
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texID);
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        
        glDepthFunc(GL_LEQUAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Build dynamic vertex buffer
        FloatBuffer buf = BufferUtils.createFloatBuffer(text.length() * 6 * 4);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(0.0f);
            FloatBuffer yb = stack.floats(0.0f);
            STBTTAlignedQuad quad = STBTTAlignedQuad.mallocStack(stack);
            for (char c : text.toCharArray()) {
                if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) continue;
                stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - FIRST_CHAR, xb, yb, quad, true);
                // Triangle 1
                buf.put(quad.x0()).put(quad.y0()).put(quad.s0()).put(quad.t0());
                buf.put(quad.x1()).put(quad.y0()).put(quad.s1()).put(quad.t0());
                buf.put(quad.x1()).put(quad.y1()).put(quad.s1()).put(quad.t1());
                // Triangle 2
                buf.put(quad.x1()).put(quad.y1()).put(quad.s1()).put(quad.t1());
                buf.put(quad.x0()).put(quad.y1()).put(quad.s0()).put(quad.t1());
                buf.put(quad.x0()).put(quad.y0()).put(quad.s0()).put(quad.t0());
            }
        }
        buf.flip();

        // Upload & draw
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        glDrawArrays(GL_TRIANGLES, 0, buf.limit() / 4);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, 0);
        glUseProgram(0);
    }

    /**
     * Render text at screen coordinates (x, y).
     */
    public void renderText(String text, float x, float y, int z, float r, float g, float b, float angle) {
        renderText(projection,text,x,y, z, r,g,b,angle);
    }

    /** Cleanup GPU resources */
    public void cleanup() {
        glDeleteTextures(texID);
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
        charData.free();
        initialized = false;
        program.cleanup();
    }

    private ByteBuffer loadFont(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.isReadable(p)) throw new IOException("Cannot read font: " + path);
        try (SeekableByteChannel ch = Files.newByteChannel(p)) {
            ByteBuffer buf = BufferUtils.createByteBuffer((int) ch.size() + 1);
            while (ch.read(buf) != -1);
            buf.flip();
            return buf;
        }
    }

    public float getStringWidth(String text) {
        // We'll simulate the text rendering to get the final x position
        float currentX = 0.0f; // Start at 0, as if rendering from the origin
        float currentY = 0.0f; // Y doesn't affect width, but GetBakedQuad requires it

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(currentX);
            FloatBuffer yb = stack.floats(currentY); // Y doesn't matter for width
            STBTTAlignedQuad quad = STBTTAlignedQuad.mallocStack(stack);
            
            for (char c : text.toCharArray()) {
                if (c < FIRST_CHAR || c >= FIRST_CHAR + CHAR_COUNT) continue;
                // The 'true' at the end means it modifies xb and yb in-place.
                stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - FIRST_CHAR, xb, yb, quad, true);
                // xb.get(0) now holds the x position for the next character
            }
            return xb.get(0) - currentX; // Return the total advance from the starting x
        }
    }

    public void renderText(Matrix4f projView, String text, Vector2i position, RGB color) {
        renderText(projView,text,position,color,0);
    }
    
    public void renderText(String text, Vector2i position, int z, RGB color, float angle) {
        renderText(text,position.x(),position.y(),z,color.getRed(),color.getGreen(),color.getBlue(), angle);
    }

    public void renderText(String text, int x, int y, int z, RGB color, float angle) {
        renderText(text,x,y,z,color.getRed(),color.getGreen(),color.getBlue(), angle);
    }

    public void renderText(Matrix4f projView, String text, Vector2i pos, RGB color, float angle) {
        renderText(projView,text,pos.x,pos.y, 0, color.r(),color.g(),color.b(), angle);
    }

    public float getFontHeight() {
        return this.fontHeight;
    }

    public void setFontHeight(int fontHeight) {
        this.fontHeight = fontHeight;
        reinit();
    }

    private void reinit() {
        cleanup();
        init();
    }

    public STBTTBakedChar.Buffer getCharData() {
        return charData;
    }

    public int getTextureID() {
        return texID;
    }

    public int getBitmapWidth() {
        return BITMAP_W;
    }

    public int getBitmapHeight() {
        return BITMAP_H;
    }

    public int getLocProj() {
        return locProj;
    }

    public int getLocTextColor() {
        return locTextColor;
    }

    public int getLocFontAtlas() {
        return locFontAtlas;
    }
    
    public ShaderProgram getProgram() {
        return program;
    }
    
    public void renderText(Matrix4f projView, String text, int x, int y, int r, int g, int b) {
        renderText(projView,text,x,y, 0, r,g,b,0);
    }
}
