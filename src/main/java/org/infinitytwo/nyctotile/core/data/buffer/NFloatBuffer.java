package org.infinitytwo.nyctotile.core.data.buffer;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * A native float buffer that behaves like a C++ dynamic array (vector-style)
 * but implemented in Java using off-heap memory.
 * <p>
 * This class allocates a manually managed {@link FloatBuffer} using
 * {@link org.lwjgl.system.MemoryUtil#memAllocFloat(int)}. It avoids the
 * Java garbage collector entirely, offering better performance and control
 * in graphics-intensive workloads such as OpenGL or Vulkan rendering.
 * </p>
 *
 * <h3>How It Works</h3>
 * <ul>
 *     <li>Allocates an initial capacity of {@code 4096} floats (4 KB).</li>
 *     <li>Automatically resizes via {@link #require(int)} when additional space is needed.</li>
 *     <li>Tracks the number of written floats to keep a clean separation between
 *     used and free space.</li>
 *     <li>Provides fast bulk writes with {@link #put(float[])},
 *     {@link #put(FloatBuffer)}, and single-element {@link #put(float)}.</li>
 * </ul>
 *
 * <h3>Memory Management</h3>
 * <p>
 * Since this buffer is manually managed (off-heap), you <strong>must</strong> call
 * {@link #cleanup()} or use this class inside a try-with-resources block
 * to avoid memory leaks:
 * </p>
 *
 * <pre>{@code
 * try (NFloatBuffer buf = new NFloatBuffer()) {
 *     buf.put(1.0f);
 *     buf.put(2.0f);
 *     FloatBuffer fb = buf.getBuffer();
 *     // ... use fb in OpenGL calls
 * } // Automatically calls buf.close() → cleanup()
 * }</pre>
 *
 * <h3>Why Use This?</h3>
 * <p>
 * Useful when you need many dynamically growing buffers (like ArrayLists)
 * but want to avoid JVM heap bloat. It’s also a great fit for batch rendering,
 * mesh data, or GPU upload buffers.
 * </p>
 *
 * <h3>Warning</h3>
 * <ul>
 *     <li>Forgetting to call {@link #cleanup()} or using this outside of
 *     try-with-resources will cause a memory leak.</li>
 *     <li>Once cleaned up, this buffer is invalid and must not be reused.</li>
 * </ul>
 *
 * @see java.nio.FloatBuffer
 * @see org.lwjgl.system.MemoryUtil
 */
public final class NFloatBuffer extends NativeBuffer {
    private FloatBuffer buffer;// 4 KB
    private static final int INITIAL = 4096;

    /**
     * Creates a new NFloatBuffer. This allocates a {@code INITIAL} of
     */
    public NFloatBuffer() {
        buffer = memAllocFloat(INITIAL);
        capacity = INITIAL;
        // In NativeBuffer, 'written' should be 0 by default.
    }

    public NFloatBuffer(int capacity) {
        buffer = memAllocFloat(capacity);
        this.capacity = capacity;
    }

    /**
     * Ensures that there is space for incoming data to be written.
     * @param c The expected required space for the data to be written.
     */
    public void require(int c) {
        if (written + c > capacity) {
            int nCapacity = Math.max(capacity * 2, written + c + INITIAL);

            FloatBuffer nBuffer = memAllocFloat(nCapacity);

            // Prepare old buffer for reading (limit = written)
            buffer.flip();
            // Bulk copy the actual written data
            nBuffer.put(buffer);

            // Update capacity field
            capacity = nCapacity;

            // Free old buffer and update reference
            memFree(buffer);
            buffer = nBuffer;

            // The new buffer is now in WRITE mode, positioned correctly at 'written'
        }
    }

    /**
     * Appends a new element to the buffer.
     * @param f The value to be put.
     */
    public void put(float f) {
        if (written >= capacity) {
            // If not enough space, perform the resize/reposition
            require(1);
        }
        buffer.put(f);
        written++;
    }

    /**
     * Appends every element in {@code b} to the native buffer.
     * @param b The buffer to be put into the native buffer.
     */
    public void put(FloatBuffer b) {
        int length = b.remaining();

        // Check capacity before calling the resize function
        if (written + length > capacity) {
            require(length);
        }

        buffer.put(b);
        written += length;
    }

    /**
     * Appends every element of {@code float[]} into the buffer.
     * @param l The list to be put into the buffer.
     */
    public void put(float[] l) {
        int length = l.length;

        // Check capacity before calling the resize function
        if (written + length > capacity) {
            require(length);
        }

        buffer.put(l);
        written += length;
    }

    /**
     * @return A buffer with position of 0 (beginning of the buffer). And
     * with a limit of current written floats
     */
    @Override
    public FloatBuffer getBuffer() {
        buffer.position(0);
        buffer.limit(written);
        return buffer;
    }

    @Override
    public int size() {
        return buffer.capacity();
    }

    /**
     * The standard method. You need to call this <strong>AFTER</strong> you finish with this buffer.
     * Also resets fields to prevent accidental double-free.
     */
    @Override
    public void cleanup() {
        if (buffer != null) {
            memFree(buffer);
            buffer = null;
            written = 0;
            capacity = 0;
        }
    }

    /**
     * Prepares the buffer for immediate reuse without memory deallocation.
     * Should be called at the start of any iteration.
     */
    @Override
    public void reset() {
        written = 0;
        // Sets position to 0, limit to capacity. Ready for writing.
        buffer.clear();
    }

    public FloatBuffer duplicate() {
        return buffer.duplicate();
    }

    public FloatBuffer limit(int newLimit) {
        return buffer.limit(newLimit);
    }

    public FloatBuffer asReadOnlyBuffer() {
        return buffer.asReadOnlyBuffer();
    }

    public FloatBuffer position(int newPosition) {
        return buffer.position(newPosition);
    }

    public int capacity() {
        return buffer.capacity();
    }

    public FloatBuffer get(int index, float[] dst) {
        return buffer.get(index, dst);
    }

    public int compareTo(FloatBuffer that) {
        return buffer.compareTo(that);
    }

    public FloatBuffer mark() {
        return buffer.mark();
    }

    public float get() {
        return buffer.get();
    }

    public FloatBuffer put(int index, float[] src, int offset, int length) {
        return buffer.put(index, src, offset, length);
    }

    public int position() {
        return buffer.position();
    }

    public int remaining() {
        return buffer.remaining();
    }

    public float get(int index) {
        return buffer.get(index);
    }

    public int mismatch(FloatBuffer that) {
        return buffer.mismatch(that);
    }

    public FloatBuffer put(int index, float[] src) {
        return buffer.put(index, src);
    }

    public FloatBuffer flip() {
        return buffer.flip();
    }

    public FloatBuffer put(int index, float f) {
        return buffer.put(index, f);
    }

    public ByteOrder order() {
        return buffer.order();
    }

    public FloatBuffer put(int index, FloatBuffer src, int offset, int length) {
        return buffer.put(index, src, offset, length);
    }

    public FloatBuffer compact() {
        return buffer.compact();
    }

    public FloatBuffer get(float[] dst, int offset, int length) {
        return buffer.get(dst, offset, length);
    }

    public FloatBuffer rewind() {
        return buffer.rewind();
    }

    public boolean isDirect() {
        return buffer.isDirect();
    }

    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }

    public float[] array() {
        return buffer.array();
    }

    public FloatBuffer slice() {
        return buffer.slice();
    }

    public FloatBuffer get(float[] dst) {
        return buffer.get(dst);
    }

    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    public boolean hasArray() {
        return buffer.hasArray();
    }

    public FloatBuffer slice(int index, int length) {
        return buffer.slice(index, length);
    }

    public FloatBuffer get(int index, float[] dst, int offset, int length) {
        return buffer.get(index, dst, offset, length);
    }

    public int limit() {
        return buffer.limit();
    }

    public FloatBuffer put(float[] src, int offset, int length) {
        return buffer.put(src, offset, length);
    }

    public int arrayOffset() {
        return buffer.arrayOffset();
    }
}