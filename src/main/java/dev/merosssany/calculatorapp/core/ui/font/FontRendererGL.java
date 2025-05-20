package dev.merosssany.calculatorapp.core.ui.font;

import dev.merosssany.calculatorapp.core.render.ShaderProgram;
import dev.merosssany.calculatorapp.core.render.Window;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

public class FontRendererGL {

    private static final int BITMAP_WIDTH = 512;
    private static final int BITMAP_HEIGHT = 512;

    private int textureId;
    private STBTTBakedChar.Buffer charData;
    private final ShaderProgram shader;
    private final int vao, vbo;
    private final int fontSize;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float colorR = 1.0f, colorG = 1.0f, colorB = 1.0f, colorA = 1.0f;
    private final File fontPath;

    public FontRendererGL(String fontPath, float fontSize, ShaderProgram shader) throws IOException {
        this.shader = shader;
        this.fontPath = new File(fontPath);
        this.fontSize = (int) fontSize;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Setup VAO and VBO
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);

        // position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        // texcoord
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

        glBindVertexArray(0);

        generateBitmap(); // Generate font bitmap and charData
    }

    public void setScale(float sx, float sy) {
        this.scaleX = sx;
        this.scaleY = sy;
    }

    public void setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
    }

    public void generateBitmap() {
        Font font;
        try (InputStream fontStream = new FileInputStream(fontPath)) {
            font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(Font.PLAIN, fontSize);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load font: " + fontPath, e);
        }

        // Create a fake image and graphics context to get font metrics.
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fontMetrics = g2d.getFontMetrics();

        int estimatedWidth = (int) Math.sqrt(font.getNumGlyphs()) * font.getSize() + 1;
        int width = 0;
        int height = fontMetrics.getHeight();
        int x = 0;
        int y = (int) (fontMetrics.getHeight() * 1.4f);
        Map<Integer, CharInfo> characterMap = new HashMap<>();

        for (int i = 0; i < font.getNumGlyphs(); i++) {
            if (font.canDisplay(i)) {
                CharInfo charInfo = new CharInfo(x, y, fontMetrics.charWidth(i), fontMetrics.getHeight());
                characterMap.put(i, charInfo);
                width = Math.max(x + fontMetrics.charWidth(i), width);
                x += charInfo.width;
                if (x > estimatedWidth) {
                    x = 0;
                    y += (int) (fontMetrics.getHeight() * 1.4f);
                    height += (int) (fontMetrics.getHeight() * 1.4f);
                }
            }
        }
        height += (int) (fontMetrics.getHeight() * 1.4f);
        g2d.dispose();

        // Create the real texture
        final int bitmapWidth = width;
        final int bitmapHeight = height;
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw each character onto the bitmap
        for (int i = 0; i < font.getNumGlyphs(); i++) {
            if (font.canDisplay(i)) {
                CharInfo info = characterMap.get(i);
                info.calculateTextureCoordinates(width, height);
                g2d.drawString("" + (char) i, info.sourceX, info.sourceY);
            }
        }
        g2d.dispose();

        // Extract pixel data from the BufferedImage into a ByteBuffer.
        ByteBuffer imageData = BufferUtils.createByteBuffer(width * height * 4); // RGBA
        int[] pixels = new int[width * height];
        img.getRGB(0, 0, width, height, pixels, 0, width);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            imageData.put((byte) ((pixel >> 16) & 0xFF)); // Red
            imageData.put((byte) ((pixel >> 8) & 0xFF));  // Green
            imageData.put((byte) (pixel & 0xFF));       // Blue
            imageData.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
        }
        imageData.flip();

        // Create the OpenGL texture.
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        // Prepare charData for stbtt_GetBakedQuad.
        ByteBuffer bitmapData = BufferUtils.createByteBuffer(width * height); // Use BufferUtils.createByteBuffer
        // Convert RGBA to RED for stbtt_BakeFontBitmap
        ByteBuffer redData = BufferUtils.createByteBuffer(width * height);
        for (int i = 0; i < width * height; i++) {
            // Get the red component from the RGBA data
            redData.put(imageData.get(i * 4));
        }
        redData.flip();

        charData = STBTTBakedChar.malloc(96);
//        stbtt_BakeFontBitmap(redData, width, height, 32, 96, charData);
    }

    public void renderText(String text, float x, float y, float sx, float sy, Window window) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (!glIsEnabled(GL_BLEND)) {
            System.err.println("WARNING: GL_BLEND is not enabled before rendering text!");
            glEnable(GL_BLEND);
        }
        if (glGetInteger(GL_BLEND_SRC_ALPHA) != GL_SRC_ALPHA || glGetInteger(GL_BLEND_DST_ALPHA) != GL_ONE_MINUS_SRC_ALPHA) {
            System.err.println("WARNING: Incorrect Blend Function, setting to SRC_ALPHA, ONE_MINUS_SRC_ALPHA");
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
        shader.bind();
        shader.setUniform1i("tex", 0);
        shader.setUniform4f("color", colorR, colorG, colorB, colorA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glBindVertexArray(vao);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(x);
            FloatBuffer yb = stack.floats(y);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c >= 128) continue;

                stbtt_GetBakedQuad(charData, BITMAP_WIDTH, BITMAP_HEIGHT, c - 32, xb, yb, quad, true);

                float x0 = quad.x0() * sx * scaleX;
                float y0 = quad.y0() * sy * scaleY;
                float x1 = quad.x1() * sx * scaleX;
                float y1 = quad.y1() * sy * scaleY;
                float s0 = quad.s0(), t0 = quad.t0();
                float s1 = quad.s1(), t1 = quad.t1();

                float[] vertices = {
                        x0, y0, s0, t0,
                        x1, y0, s1, t0,
                        x1, y1, s1, t1,

                        x0, y0, s0, t0,
                        x1, y1, s1, t1,
                        x0, y1, s0, t1,
                };

                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }
        }

        glBindVertexArray(0);
        shader.unbind();
    }

    public void cleanup() {
        glDeleteTextures(textureId);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        if (charData != null)
            charData.free();
    }

    private void saveBitmapToPNG(ByteBuffer bitmap, int width, int height, String filename) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, (bitmap.get(y * width + x) & 0xFF) << 16 | (bitmap.get(y * width + x) & 0xFF) << 8 | (bitmap.get(y * width + x) & 0xFF));
            }
        }
        File output = new File(filename);
        ImageIO.write(image, "png", output);
        System.out.println("Baked font bitmap saved to: " + filename);
    }

    static class CharInfo {
        public int sourceX, sourceY, width, height;
        public Vector2f[] textureCoordinates;

        public CharInfo(int x, int y, int width, int height) {
            this.sourceX = x;
            this.sourceY = y;
            this.width = width;
            this.height = height;
            this.textureCoordinates = new Vector2f[4]; // Initialize the array
        }

        public void calculateTextureCoordinates(int fontWidth, int fontHeight) {
            float x0 = (float) sourceX / (float) fontWidth;
            float x1 = (float) (sourceX + width) / (float) fontWidth;
            float y0 = (float) (sourceY) / (float) fontHeight;
            float y1 = (float) (sourceY + height) / (float) fontHeight;

            textureCoordinates[0] = new Vector2f(x0, y0); // Top-left
            textureCoordinates[1] = new Vector2f(x1, y0); // Top-right
            textureCoordinates[2] = new Vector2f(x1, y1); // Bottom-right
            textureCoordinates[3] = new Vector2f(x0, y1); // Bottom-left
        }
    }
}
