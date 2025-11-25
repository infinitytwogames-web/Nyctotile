package org.infinitytwo.nyctotile.core.model;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap; // For mipmaps

public class Texture {

    private final int textureID;
    private int width, height;

    public Texture(String filePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Set texture wrapping parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); // S coordinate
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT); // T coordinate

        // Set texture filtering parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); // Mipmap filtering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); // Magnification filtering

        ByteBuffer imageBuffer;
        try {
            // Attempt to load from classpath resources first, then filesystem
            imageBuffer = loadResourceToByteBuffer(filePath);
        } catch (IOException e) {
            System.err.println("Failed to load texture from resource path: " + filePath + ". Trying filesystem path.");
            // Fallback to direct file system loading if resource path fails
            try {
                imageBuffer = ioResourceToByteBuffer(filePath, 2048); // Initial buffer size
            } catch (IOException ex) {
                throw new RuntimeException("Failed to load texture from either resource or filesystem: " + filePath, ex);
            }
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load image using STBImage
            STBImage.stbi_set_flip_vertically_on_load(true); // Flip texture vertically
            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4); // Load as RGBA
            if (image == null) {
                throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
            }

            this.width = w.get(0);
            this.height = h.get(0);
            // Upload image data to OpenGL
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D); // Generate mipmaps for optimized rendering at different distances

            STBImage.stbi_image_free(image); // Free the image buffer after uploading to GPU
        }
    }

    public Texture(BufferedImage image) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        this.width = image.getWidth();
        this.height = image.getHeight();

        // Convert BufferedImage to RGBA ByteBuffer
        ByteBuffer buffer = convertImageToRGBA(image);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        // Upload to OpenGL
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    /**
     * Flips a BufferedImage horizontally (along the Y-axis).
     *
     * @param originalImage The BufferedImage to flip.
     * @return A new BufferedImage that is the horizontal mirror of the original.
     */
    public static BufferedImage flipXAxis(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage flippedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g2d = flippedImage.createGraphics();

        // 3. Create the AffineTransform for the vertical flip
        AffineTransform tx = new AffineTransform();

        // Scale by 1.0 in the X direction (no change horizontally)
        // Scale by -1.0 in the Y direction (the vertical flip)
        tx.scale(1.0, -1.0);

        // Translate the image back into the positive coordinate space (downward)
        // by moving it down by its height. Scaling by -1.0 in Y moves the image
        // from y=0..height to y=-height..0. We shift it down by -height to get
        // it back to y=0..height.
        tx.translate(0, -height);

        // 4. Apply the transform and draw
        g2d.transform(tx);
        g2d.drawImage(originalImage, 0, 0, null);

        g2d.dispose();

        return flippedImage;
    }

    private ByteBuffer convertImageToRGBA(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];

                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }

        buffer.flip();
        return buffer;
    }


    public void bind() { // This is what's called
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }

    // Helper method to load resource into a ByteBuffer, useful for textures.
    // This tries to load from the classpath, which is generally better for packaged games.
    private ByteBuffer loadResourceToByteBuffer(String resourcePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (var is = classLoader.getResourceAsStream(resourcePath);
            ReadableByteChannel rbc = Channels.newChannel(is)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            ByteBuffer buffer = BufferUtils.createByteBuffer(2048); // Initial capacity
            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) break;
                if (buffer.remaining() == 0) {
                    buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                }
            }
            buffer.flip();
            return buffer;
        }
    }

    // Fallback if loadResourceToByteBuffer fails, for direct file system paths
    private ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        Path path = Paths.get(resource);
        if (Files.isReadable(path)) {
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = BufferUtils.createByteBuffer((int)fc.size() + 1);
                while (fc.read(buffer) != -1) ;
            }
        } else {
            try (
                    ReadableByteChannel rbc = Channels.newChannel(getClass().getClassLoader().getResourceAsStream(resource))) {
                buffer = BufferUtils.createByteBuffer(bufferSize);
                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) break;
                    if (buffer.remaining() == 0) {
                        buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                    }
                }
            }
        }
        buffer.flip();
        return buffer;
    }

    private ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getTextureID() {
        return textureID;
    }
}