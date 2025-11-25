package org.infinitytwo.nyctotile.core.data.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.lwjgl.BufferUtils.createByteBuffer;

public class ResourceLoader {

    /**
     * Loads a resource into a ByteBuffer, handling both file system and classpath resources.
     *
     * @param resource The path to the resource (e.g., "textures/icon.png").
     * @param bufferSize The initial buffer size. Will be resized if needed.
     * @return A ByteBuffer containing the resource data.
     * @throws IOException If the resource cannot be read.
     */
    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        Path path = Paths.get(resource); // Try as a file system path first

        if (Files.isReadable(path)) {
            // Resource is directly on the file system
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = createByteBuffer((int)fc.size() + 1);
                while (fc.read(buffer) != -1);
            }
        } else {
            // Resource is likely on the classpath (e.g., inside a JAR)
            try (InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                 ReadableByteChannel rbc = Channels.newChannel(source)) {
                if (source == null) {
                    throw new IOException("Resource not found: " + resource);
                }
                buffer = createByteBuffer(bufferSize); // Start with a default buffer size
                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    // If buffer is full, resize it
                    if (buffer.remaining() == 0) {
                        buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2);
                    }
                }
            }
        }

        buffer.flip(); // Prepare for reading
        return buffer;
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = createByteBuffer(newCapacity);
        buffer.flip(); // Prepare old buffer for reading
        newBuffer.put(buffer); // Copy contents
        return newBuffer;
    }
}