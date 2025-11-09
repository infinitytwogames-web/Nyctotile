package org.infinitytwo.umbralore.core.data.buffer;

import org.jetbrains.annotations.NotNull;

import java.nio.*;

import static org.lwjgl.system.MemoryUtil.*;

public class NByteBuffer extends NativeBuffer {
    private ByteBuffer buffer;
    
    public NByteBuffer() {
        buffer = memAlloc(INITIAL);
        
        capacity = INITIAL;
    }
    
    public NByteBuffer(int length) {
        buffer = memAlloc(length);
        
        capacity = length;
    }
    
    /**
     * Ensures the buffer has at least 'requiredCapacity' space. If not,
     * a new, larger buffer is allocated, data is copied, and the old buffer is freed.
     */
    public void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity <= buffer.capacity()) {
            return;
        }
        
        int newCapacity = buffer.capacity() * 2;
        if (newCapacity < requiredCapacity) {
            newCapacity = requiredCapacity;
        }
        
        // Save current position and limit
        int currentPosition = buffer.position();
        int currentLimit = buffer.limit();
        
        // 1. Allocate the new, larger native buffer
        ByteBuffer newBuffer = memAlloc(newCapacity);
        
        // 2. Prepare the old buffer for reading (flip it to read all content)
        buffer.flip();
        
        // 3. Copy content from old buffer to new buffer
        newBuffer.put(buffer);
        
        // 4. Restore the state (position/limit) of the new buffer
        newBuffer.limit(newCapacity);
        newBuffer.position(currentPosition);
        
        // 5. Free the old native buffer
        memFree(this.buffer);
        
        // 6. Update references
        this.buffer = newBuffer;
        this.capacity = newCapacity;
    }
    
    public ByteBuffer put(byte b) {
        // 1. Check if we need space for 1 byte at the current position
        ensureCapacity(buffer.position() + 1);
        
        // 2. Perform the put operation on the (potentially new) buffer
        return buffer.put(b);
    }
    
    @Override
    public int size() {
        return capacity;
    }
    
    @Override
    public void cleanup() {
        // CRITICAL FIX: Free the native memory allocated with memAlloc
        memFree(buffer);
        // Set to null to prevent dangling reference
        this.buffer = null;
        this.capacity = 0;
    }
    
    @Override
    public Buffer getBuffer() {
        // CRITICAL FIX: Return the actual ByteBuffer, not null.
        return buffer;
    }
    
    @Override
    public void reset() {
        // Commonly used to reset position and limit for reuse
        buffer.clear();
    }
    
    public int getInt(int index) {
        return buffer.getInt(index);
    }
    
    public ByteBuffer slice(int index, int length) {
        return buffer.slice(index, length);
    }
    
    public int limit() {
        return buffer.limit();
    }
    
    public ByteBuffer mark() {
        return buffer.mark();
    }
    
    public short getShort() {
        return buffer.getShort();
    }
    
    public ByteBuffer clear() {
        return buffer.clear();
    }
    
    public ByteOrder order() {
        return buffer.order();
    }
    
    public IntBuffer asIntBuffer() {
        return buffer.asIntBuffer();
    }
    
    public int capacity() {
        return buffer.capacity();
    }
    
    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }
    
    public double getDouble() {
        return buffer.getDouble();
    }
    
    public ByteBuffer get(byte[] dst, int offset, int length) {
        return buffer.get(dst, offset, length);
    }
    
    public ByteBuffer compact() {
        return buffer.compact();
    }
    
    public long getLong(int index) {
        return buffer.getLong(index);
    }
    
    public ByteBuffer alignedSlice(int unitSize) {
        return buffer.alignedSlice(unitSize);
    }
    
    public int getInt() {
        return buffer.getInt();
    }
    
    public LongBuffer asLongBuffer() {
        return buffer.asLongBuffer();
    }
    
    public ByteBuffer get(int index, byte[] dst, int offset, int length) {
        return buffer.get(index, dst, offset, length);
    }
    
    public byte[] array() {
        return buffer.array();
    }
    
    public ByteBuffer asReadOnlyBuffer() {
        return buffer.asReadOnlyBuffer();
    }
    
    public ByteBuffer position(int newPosition) {
        return buffer.position(newPosition);
    }
    
    public ByteBuffer duplicate() {
        return buffer.duplicate();
    }
    
    public ByteBuffer get(int index, byte[] dst) {
        return buffer.get(index, dst);
    }
    
    public int compareTo(ByteBuffer that) {
        return buffer.compareTo(that);
    }
    
    public float getFloat(int index) {
        return buffer.getFloat(index);
    }
    
    public ByteBuffer limit(int newLimit) {
        return buffer.limit(newLimit);
    }
    
    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }
    
    public CharBuffer asCharBuffer() {
        return buffer.asCharBuffer();
    }
    
    public ByteBuffer slice() {
        return buffer.slice();
    }
    
    public int mismatch(ByteBuffer that) {
        return buffer.mismatch(that);
    }
    
    public long getLong() {
        return buffer.getLong();
    }
    
    public FloatBuffer asFloatBuffer() {
        return buffer.asFloatBuffer();
    }
    
    public ByteBuffer flip() {
        return buffer.flip();
    }
    
    public ByteBuffer order(@NotNull ByteOrder bo) {
        return buffer.order(bo);
    }
    
    public int position() {
        return buffer.position();
    }
    
    public int remaining() {
        return buffer.remaining();
    }
    
    public int alignmentOffset(int index, int unitSize) {
        return buffer.alignmentOffset(index, unitSize);
    }
    
    public ByteBuffer rewind() {
        return buffer.rewind();
    }
    
    public double getDouble(int index) {
        return buffer.getDouble(index);
    }
    
    public short getShort(int index) {
        return buffer.getShort(index);
    }
    
    public boolean isDirect() {
        return buffer.isDirect();
    }
    
    public byte get(int index) {
        return buffer.get(index);
    }
    
    public boolean hasArray() {
        return buffer.hasArray();
    }
    
    public ByteBuffer get(byte[] dst) {
        return buffer.get(dst);
    }
    
    public float getFloat() {
        return buffer.getFloat();
    }
    
    public DoubleBuffer asDoubleBuffer() {
        return buffer.asDoubleBuffer();
    }
    
    public ShortBuffer asShortBuffer() {
        return buffer.asShortBuffer();
    }
    
    public byte get() {
        return buffer.get();
    }
    
    public char getChar() {
        return buffer.getChar();
    }
    
    public int arrayOffset() {
        return buffer.arrayOffset();
    }
    
    public char getChar(int index) {
        return buffer.getChar(index);
    }
    
    public ByteBuffer put(byte[] src) {
        ensureCapacity(buffer.position() + src.length);
        return buffer.put(src);
    }
    
    public ByteBuffer putLong(long value) {
        ensureCapacity(buffer.position() + Long.BYTES); // Corrected size check order
        return buffer.putLong(value);
    }
    
    public ByteBuffer putFloat(float value) {
        ensureCapacity(buffer.position() + Float.BYTES);
        return buffer.putFloat(value);
    }
    
    public ByteBuffer putChar(char value) {
        ensureCapacity(buffer.position() + Character.BYTES);
        return buffer.putChar(value);
    }
    
    public ByteBuffer putDouble(double value) {
        ensureCapacity(buffer.position() + Double.BYTES);
        return buffer.putDouble(value);
    }
    
    public ByteBuffer putShort(short value) {
        ensureCapacity(buffer.position() + Short.BYTES);
        return buffer.putShort(value);
    }
    
    public ByteBuffer put(ByteBuffer src) {
        ensureCapacity(buffer.position() + src.remaining()); // Must check src.remaining()
        return buffer.put(src);
    }
    
    public ByteBuffer put(byte[] src, int offset, int length) {
        ensureCapacity(buffer.position() + length); // Check against the 'length' being written
        return buffer.put(src, offset, length);
    }
    
    public ByteBuffer putInt(int value) {
        ensureCapacity(buffer.position() + Integer.BYTES);
        return buffer.putInt(value);
    }


// --- INDEXED PUTS (Check index + size) ---
    
    public ByteBuffer putChar(int index, char value) {
        ensureCapacity(index + Character.BYTES); // Corrected to use 'index'
        return buffer.putChar(index, value);
    }
    
    public ByteBuffer putFloat(int index, float value) {
        ensureCapacity(index + Float.BYTES); // Added
        return buffer.putFloat(index, value);
    }
    
    public ByteBuffer putShort(int index, short value) {
        ensureCapacity(index + Short.BYTES); // Corrected to use 'index'
        return buffer.putShort(index, value);
    }
    
    public ByteBuffer putDouble(int index, double value) {
        ensureCapacity(index + Double.BYTES); // Corrected to use 'index'
        return buffer.putDouble(index, value);
    }
    
    public ByteBuffer put(int index, byte[] src) {
        ensureCapacity(index + src.length); // Added
        return buffer.put(index, src);
    }
    
    public ByteBuffer put(int index, byte b) {
        ensureCapacity(index + 1); // Added (single byte indexed put)
        return buffer.put(index, b);
    }
    
    public ByteBuffer put(int index, byte[] src, int offset, int length) {
        ensureCapacity(index + length); // Corrected to use 'index' and 'length'
        return buffer.put(index, src, offset, length);
    }
    
    public ByteBuffer putInt(int index, int value) {
        ensureCapacity(index + Integer.BYTES); // Corrected to use 'index'
        return buffer.putInt(index, value);
    }
    
    public ByteBuffer putLong(int index, long value) {
        ensureCapacity(index + Long.BYTES); // Corrected to use 'index'
        return buffer.putLong(index, value);
    }
    
    public ByteBuffer put(int index, ByteBuffer src, int offset, int length) {
        ensureCapacity(index + length); // Corrected to use 'index' and 'length'
        return buffer.put(index, src, offset, length);
    }
}
