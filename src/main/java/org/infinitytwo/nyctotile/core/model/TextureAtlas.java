package org.infinitytwo.nyctotile.core.model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.GL_MAX_TEXTURE_SIZE;
import static org.lwjgl.opengl.GL11.glGetInteger;

public class TextureAtlas {
    protected int rows;
    protected int columns;
    protected int atlasWidth, atlasHeight, imageWidth, imageHeight;
    protected final ArrayList<BufferedImage> images = new ArrayList<>();
    protected Texture plain;
    private boolean isBuilt = false;

    /**
     * Creates a new TextureAtlas class.
     *
     * @param rows    The rows of the TextureAtlas (in images)
     * @param columns The columns of the TextureAtlas (in images)
     */
    public TextureAtlas(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    /**
     * Binds a new texture to the atlas.
     *
     * @param path The path of the image. (full or relative to working directory)
     * @param flip Flip the image vertically or not.
     * @return Texture index which then used to get correct UV coordinates
     * @throws IOException           If the image was not found.
     * @throws IllegalStateException If the image does not match the others or the TextureAtlas is full.
     */
    public int addTexture(String path, boolean flip) throws IOException, IllegalStateException {
        BufferedImage image = ImageIO.read(new File(path));
        if (flip) image = Texture.flipXAxis(image); // Why this doesn't take effect?
        if (image == null) throw new IOException("Failed to get the image: " + path);
        if (images.size() + 1 > (rows * columns))
            throw new IllegalStateException("TextureAtlas is full and cannot add new elements");
        if (imageWidth == 0 && imageHeight == 0) {
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            images.add(image);
        } else if (imageWidth == image.getWidth() && imageHeight == image.getHeight()) images.add(image);
        else throw new IllegalStateException("The image must match the size: " + imageWidth + "x" + imageHeight);
        return images.size()-1;
    }

    public void addTexture(BufferedImage image) throws IllegalStateException {
        if (image == null) throw new IllegalArgumentException("Image cannot be null");
        if (images.size() + 1 > (rows * columns))
            throw new IllegalStateException("TextureAtlas is full and cannot add new elements");

        if (imageWidth == 0 && imageHeight == 0) {
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            images.add(image);
        } else if (imageWidth == image.getWidth() && imageHeight == image.getHeight()) {
            images.add(image);
        } else {
            throw new IllegalStateException("The image must match the size: " + imageWidth + "x" + imageHeight);
        }
    }

    public Texture build() {
        atlasWidth = imageWidth * columns;
        atlasHeight = imageHeight * rows;

        int maxSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        if (atlasWidth > maxSize || atlasHeight > maxSize) {
            throw new IllegalStateException("TextureAtlas too large for GPU: " + maxSize);
        }

        if (plain != null) {
            plain.cleanup();
            plain = null;
        }

        BufferedImage atlas = new BufferedImage(imageWidth * columns, imageHeight * rows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphic = atlas.createGraphics();

        int row = 0, column = 0;

        // Store the original transform to reset it later
        AffineTransform originalTransform = graphic.getTransform();

        for (BufferedImage image : images) {
            if (column >= columns) {
                row++;
                column = 0;
            }

            int drawX = imageWidth * column;
            int drawY = imageHeight * row;

            // --- START IMAGE FLIP ---
            // 1. Reset the transform for the current image draw
            graphic.setTransform(originalTransform);

            // 2. Translate to the start position (drawX, drawY)
            graphic.translate(drawX, drawY);

            // 3. Scale by (1, -1) to flip on the Y-axis (vertical flip)
            graphic.scale(1, -1);

            // 4. Translate back up by the image height because scaling by -1 shifts the image up
            //    by its height relative to the translated origin.
            graphic.translate(0, -imageHeight);

            // Draw the image. It will be drawn flipped into the atlas.
            graphic.drawImage(image, 0, 0, null);
            // --- END IMAGE FLIP ---

            column++;
        }

        // Restore the original transform state (identity matrix)
        graphic.setTransform(originalTransform);
        isBuilt = true;
        graphic.dispose();
        try {
            ImageIO.write(atlas,"png",new File("e.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        plain = new Texture(atlas);
        return plain;
    }

    /**
     * Returns the UV coordinates for the texture at a specific index in the atlas.
     *
     * @param textureIndex The 0-indexed position of the texture in the order it was added.
     * @return A float array {uMin, vMin, uMax, vMax} representing the normalized UV coordinates.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public float[] getUVCoords(int textureIndex) {
        if (textureIndex < 0 || textureIndex >= images.size()) {
            throw new IndexOutOfBoundsException("Texture index " + textureIndex + " out of bounds for atlas containing " + images.size() + " textures.");
        }

        int currentColumn = textureIndex % columns;
        int currentRow = textureIndex / columns;

        float uMin = (float) (currentColumn * imageWidth) / atlasWidth;
        float uMax = (float) ((currentColumn + 1) * imageWidth) / atlasWidth;

        // Since the image was flipped when drawn into the atlas (row 0 at the top, but content is flipped),
        // we use the standard OpenGL UV-flip logic: V=0 is bottom, V=1 is top.
        // We calculate the UVs as if the atlas was drawn *unflipped* from a Java perspective,
        // and then apply the OpenGL V-flip.

        // This is the V-flip needed for OpenGL when the Java image is built top-down (row 0 at Y=0)
        float vMax = 1.0f - ((float) (currentRow * imageHeight) / atlasHeight);
        float vMin = 1.0f - ((float) ((currentRow + 1) * imageHeight) / atlasHeight);

        return new float[]{uMin, vMin, uMax, vMax}; // [u0, v0, u1, v1]
    }


    public Texture getTexture() {
        if (plain == null) throw new IllegalStateException("Atlas texture has not been built yet.");
        return plain;
    }

    public void setRows(int rows) {
        if (isBuilt) throw new IllegalStateException("Cannot change rows after atlas has been built.");
        this.rows = rows;
    }

    public void setColumns(int columns) {
        if (isBuilt) throw new IllegalStateException("Cannot change columns after atlas has been built.");
        this.columns = columns;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }

    public void clear() {
        images.clear();
        imageWidth = 0;
        imageHeight = 0;
        atlasWidth = 0;
        atlasHeight = 0;
        if (plain != null) plain.cleanup();
        plain = null;
        isBuilt = false;
    }

    public boolean isBuilt() {
        return isBuilt;
    }
}
