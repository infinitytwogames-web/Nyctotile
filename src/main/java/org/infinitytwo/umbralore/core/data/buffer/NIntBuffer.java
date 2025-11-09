package org.infinitytwo.umbralore.core.data.buffer;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * A native int buffer that behaves like a C++ dynamic array (vector-style)
 * but implemented in Java using off-heap memory.
 * <p>
 * This class allocates a manually managed {@link IntBuffer} using
 * {@link org.lwjgl.system.MemoryUtil#memAllocFloat(int)}. It avoids the
 * Java garbage collector entirely, offering better performance and control
 * in graphics-intensive workloads such as OpenGL or Vulkan rendering.
 * </p>
 *
 * <h3>How It Works</h3>
 * <ul>
 *     <li>Allocates an initial capacity of {@code 4096} ints (4 KB).</li>
 *     <li>Automatically resizes via {@link #require(int)} when additional space is needed.</li>
 *     <li>Tracks the number of written ints to keep a clean separation between
 *     used and free space.</li>
 *     <li>Provides fast bulk writes with {@link #put(int[])},
 *     {@link #put(IntBuffer)}, and single-element {@link #put(int)}.</li>
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
 * try (NIntBuffer buf = new NIntBuffer()) {
 *     buf.put(1.0f);
 *     buf.put(2.0f);
 *     IntBuffer fb = buf.getBuffer();
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
 * @see IntBuffer
 * @see org.lwjgl.system.MemoryUtil
 */
@Deprecated
public final class NIntBuffer extends NativeBuffer {
    private IntBuffer buffer;

    private static final int INITIAL = 4096; // 4 KB

    /**
     * Creates a new NIntBuffer. This allocates a {@code INITIAL} of
     */
    public NIntBuffer() {
        buffer = memAllocInt(INITIAL);
        capacity = INITIAL;
        // In NativeBuffer, 'written' should be 0 by default.
    }

    public NIntBuffer(int capacity) {
        buffer = memAllocInt(capacity);
        this.capacity = capacity;
    }

    /**
     * Ensures that there is space for incoming data to be written.
     * @param c The expected required space for the data to be written.
     */
    public void require(int c) {
        if (written + c > capacity) {
            int nCapacity = Math.max(capacity * 2, written + c + INITIAL);

            IntBuffer nBuffer = memAllocInt(nCapacity);

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
     * @param i The value to be put.
     */
    public void put(int i) {
        if (written >= capacity) {
            // If not enough space, perform the resize/reposition
            require(1);
        }
        buffer.put(i);
        written++;
    }

    /**
     * Appends every element in {@code b} to the native buffer.
     * @param b The buffer to be put into the native buffer.
     */
    public void put(IntBuffer b) {
        int length = b.remaining();

        // Check capacity before calling the resize function
        if (written + length > capacity) {
            require(length);
        }

        buffer.put(b);
        written += length;
    }

    /**
     * Appends every element of {@code int[]} into the buffer.
     * @param l The list to be put into the buffer.
     */
    public void put(int[] l) {
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
     * with a limit of current written ints
     */
    @Override
    public IntBuffer getBuffer() {
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

    public IntBuffer duplicate() {
        return buffer.duplicate();
    }

    public IntBuffer limit(int newLimit) {
        return buffer.limit(newLimit);
    }

    public IntBuffer asReadOnlyBuffer() {
        return buffer.asReadOnlyBuffer();
    }

    public IntBuffer position(int newPosition) {
        return buffer.position(newPosition);
    }

    public int capacity() {
        return buffer.capacity();
    }

    public IntBuffer get(int index, int[] dst) {
        return buffer.get(index, dst);
    }

    public int compareTo(IntBuffer that) {
        return buffer.compareTo(that);
    }

    public IntBuffer mark() {
        return buffer.mark();
    }

    public int get() {
        return buffer.get();
    }

    public IntBuffer put(int index, int[] src, int offset, int length) {
        return buffer.put(index, src, offset, length);
    }

    public int position() {
        return buffer.position();
    }

    public int remaining() {
        return buffer.remaining();
    }

    public int get(int index) {
        if (index < 0 || index >= written) {
            // Checking against 'written' is the CRITICAL protection against reading garbage.
            // Reading index 32767 is valid, but reading index 32768 is NOT.
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds [0, " + written + ")");
        }
        // We can rely on the underlying buffer's integrity since we checked the boundary.
        return buffer.get(index);
    }

    public int mismatch(IntBuffer that) {
        return buffer.mismatch(that);
    }

    public IntBuffer put(int index, int[] src) {
        return buffer.put(index, src);
    }

    public IntBuffer flip() {
        return buffer.flip();
    }

    public IntBuffer put(int index, int f) {
        return buffer.put(index, f);
    }

    public ByteOrder order() {
        return buffer.order();
    }

    public IntBuffer put(int index, IntBuffer src, int offset, int length) {
        return buffer.put(index, src, offset, length);
    }

    public IntBuffer compact() {
        return buffer.compact();
    }

    public IntBuffer get(int[] dst, int offset, int length) {
        return buffer.get(dst, offset, length);
    }

    public IntBuffer rewind() {
        return buffer.rewind();
    }

    public boolean isDirect() {
        return buffer.isDirect();
    }

    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }

    public int[] array() {
        // 1. Create a new array sized exactly to the data that has been written.
        int[] array = new int[written];

        // 2. Prepare the internal IntBuffer for a bulk read operation.
        // getBuffer() already sets position(0) and limit(written).
        IntBuffer tempBuffer = getBuffer();

        // 3. Perform the bulk copy from native memory to the Java array.
        tempBuffer.get(array);

        // 4. Reset the internal buffer's state (optional but recommended
        // to maintain consistency if getBuffer() modified it).
        buffer.position(written);

        return array;
    }

    public IntBuffer slice() {
        return buffer.slice();
    }

    public IntBuffer get(int[] dst) {
        return buffer.get(dst);
    }

    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    public boolean hasArray() {
        return buffer.hasArray();
    }

    public IntBuffer slice(int index, int length) {
        return buffer.slice(index, length);
    }

    public IntBuffer get(int index, int[] dst, int offset, int length) {
        return buffer.get(index, dst, offset, length);
    }

    public int limit() {
        return buffer.limit();
    }

    public IntBuffer put(int[] src, int offset, int length) {
        return buffer.put(src, offset, length);
    }

    public int arrayOffset() {
        return buffer.arrayOffset();
    }

    public void put(NIntBuffer buffer) {
        put(buffer.buffer);
    }
}