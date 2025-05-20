package dev.merosssany.calculatorapp.core.ui.font;

import dev.merosssany.calculatorapp.core.render.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * Shader-based FontRenderer using dynamic VBO/VAO. No fixed-function matrices.
 * Pass the compiled shader program ID in the constructor.
 */
public class FontRenderer {
    private static final int BITMAP_W   = 512;
    private static final int BITMAP_H   = 512;
    private static final int FIRST_CHAR = 32;
    private static final int CHAR_COUNT = 96;

    private final int texID;
    private final STBTTBakedChar.Buffer charData;
    private final ByteBuffer bitmap;

    // Shader and uniforms
    private final int shaderProgramId;
    private final int locProj;
    private final int locTextColor;
    private final int locFontAtlas;

    // VAO/VBO for vertex data (x,y,s,t)
    private final int vaoId;
    private final int vboId;
    private ShaderProgram program;

    /**
     * @param fontPath        path to TTF file
     * @param fontHeight      pixel height
     */
    public FontRenderer(String fontPath, float fontHeight) {
        program = new ShaderProgram(
                "#version 330 core\n" +
                "layout(location = 0) in vec2 inPos;\n" +
                "layout(location = 1) in vec2 inUV;\n" +
                "uniform mat4 uProj;\n" +
                "out vec2 vUV;\n" +
                "void main() {\n" +
                "    gl_Position = uProj * vec4(inPos, 0, 1);\n" +
                "    vUV = inUV;\n" +
                "}\n" +
                "\n", "#version 330 core\n" +
                "in vec2 vUV;\n" +
                "uniform sampler2D uFontAtlas;\n" +
                "uniform vec3 uTextColor;\n" +
                "out vec4 fragColor;\n" +
                "void main(){\n" +
                "    float alpha = texture(uFontAtlas, vUV).r;\n" +
                "    fragColor = vec4(uTextColor, alpha);\n" +
                "}\n");

        this.shaderProgramId = program.getProgramId();
        // Query uniforms once
        locProj      = glGetUniformLocation(shaderProgramId, "uProj");
        locTextColor = glGetUniformLocation(shaderProgramId, "uTextColor");
        locFontAtlas = glGetUniformLocation(shaderProgramId, "uFontAtlas");

        // Load font data
        ByteBuffer fontBuffer;
        try {
            fontBuffer = loadFont(fontPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + fontPath, e);
        }

        // Bake glyphs
        charData = STBTTBakedChar.malloc(CHAR_COUNT);
        bitmap   = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);
        stbtt_BakeFontBitmap(fontBuffer, fontHeight, bitmap, BITMAP_W, BITMAP_H, FIRST_CHAR, charData);

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
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * Render text at screen coordinates (x, y).
     */
    public void renderText(Matrix4f proj, String text, float x, float y, float r, float g, float b) {
        glUseProgram(shaderProgramId);
        // Upload projection
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mat = stack.mallocFloat(16);
            proj.get(mat);
            glUniformMatrix4fv(locProj, false, mat);
        }
        // Color & sampler
        glUniform3f(locTextColor, r, g, b);
        glUniform1i(locFontAtlas, 0);

        // Bind atlas
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Build dynamic vertex buffer
        FloatBuffer buf = BufferUtils.createFloatBuffer(text.length() * 6 * 4);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(x);
            FloatBuffer yb = stack.floats(y);
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

    /** Cleanup GPU resources */
    public void cleanup() {
        glDeleteTextures(texID);
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
        charData.free();
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
}
