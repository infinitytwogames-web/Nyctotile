package dev.merosssany.calculatorapp.core.ui.font;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL11.glGenTextures;

public class TextRenderer {
    private static final float LINE_SPACING_FACTOR = 1.2f; // Changed from 1.4 to 1.2 which is the value Java uses.
    private final String filepath;
    private final int fontSize;
    private Color characterColor = Color.WHITE;
    private int width, height, lineHeight;
    private final Map<Integer, CharacterInfo> characterMap;
    public int textureId;
    private Color fontColor = Color.WHITE; // Default font color
    private static final Logger LOGGER = Logger.getLogger(TextRenderer.class.getName());

    public TextRenderer(String filepath, int fontSize) {
        this.filepath = filepath;
        this.fontSize = fontSize;
        this.characterMap = new HashMap<>();
        generateBitmap();
    }

    public TextRenderer(String filepath, int fontSize, Color fontColor) {
        this.filepath = filepath;
        this.fontSize = fontSize;
        this.characterColor = fontColor;
        this.characterMap = new HashMap<>();
        generateBitmap();
    }

    public CharacterInfo getCharacter(int codepoint) {
        return characterMap.getOrDefault(codepoint, new CharacterInfo(0, 0, 0, 0));
    }

    public void generateBitmap() {
        Font font = null;
        try {
            font = new Font(filepath, Font.PLAIN, fontSize);
        } catch (Exception e) {
            LOGGER.severe("Failed to load font: " + filepath + " : " + e.getMessage());
            throw new RuntimeException("Failed to load font: " + filepath, e);
        }

        // Create fake image to get font information
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fontMetrics = g2d.getFontMetrics();

        int estimatedWidth = (int) Math.sqrt(font.getNumGlyphs()) * font.getSize() + 1;
        width = 0;
        height = fontMetrics.getHeight();
        lineHeight = fontMetrics.getHeight();
        int x = 0;
        int y = (int) (fontMetrics.getHeight() * LINE_SPACING_FACTOR);

        for (int i = 0; i < font.getNumGlyphs(); i++) {
            if (font.canDisplay(i)) {
                // Get the sizes for each codepoint glyph, and update the actual image width and height
                CharacterInfo charInfo = new CharacterInfo(x, y, fontMetrics.charWidth(i), fontMetrics.getHeight());
                characterMap.put(i, charInfo);
                width = Math.max(x + fontMetrics.charWidth(i), width);
                x += charInfo.width;
                if (x > estimatedWidth) {
                    x = 0;
                    y += (int) (fontMetrics.getHeight() * LINE_SPACING_FACTOR);
                    height += (int) (fontMetrics.getHeight() * LINE_SPACING_FACTOR);
                }
            }
        }
        height += (int) (fontMetrics.getHeight() * LINE_SPACING_FACTOR);
        g2d.dispose();

        // Create the real texture
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(font);
        g2d.setColor(fontColor); // Use the font color
        for (int i = 0; i < font.getNumGlyphs(); i++) {
            if (font.canDisplay(i)) {
                CharacterInfo info = characterMap.get(i);
                info.calculatePosition(width, height);
                g2d.drawString("" + (char) i, info.sourceX, info.sourceY);
            }
        }
        g2d.dispose();

        uploadTexture(img);
    }

    private void uploadTexture(BufferedImage image) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        int[] pixels = new int[imgHeight * imgWidth];
        image.getRGB(0, 0, imgWidth, imgHeight, pixels, 0, imgWidth);

        ByteBuffer buffer = BufferUtils.createByteBuffer(imgWidth * imgHeight); // Use correct size for GL_ALPHA

        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                int pixel = pixels[y * imgWidth + x];
                byte alpha = (byte) ((pixel >> 24) & 0xFF);
                buffer.put(alpha);
            }
        }
        buffer.flip();

        textureId = glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, imgWidth, imgHeight,
                0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, buffer);
        buffer.clear();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void setFontColor(Color color) {
        this.fontColor = color;
        generateBitmap(); // Re-generate the bitmap with the new color
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public int getTextureId() {
        return textureId;
    }
}

