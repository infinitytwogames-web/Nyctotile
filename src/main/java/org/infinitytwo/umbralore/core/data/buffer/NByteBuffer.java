package org.infinitytwo.umbralore.core.data.buffer;

import org.jetbrains.annotations.NotNull;

import java.nio.*;

import static org.lwjgl.system.MemoryUtil.memAlloc;

@Deprecated
public class NByteBuffer extends NativeBuffer {
    private final ByteBuffer buffer;

    public NByteBuffer() {
        buffer = memAlloc(INITIAL);

        capacity = INITIAL;
    }

    public ByteBuffer putChar(int index, char value) {
        return buffer.putChar(index, value);
    }

    public ByteBuffer put(byte[] src) {
        return buffer.put(src);
    }

    public int getInt(int index) {
        return buffer.getInt(index);
    }

    public ByteBuffer slice(int index, int length) {
        return buffer.slice(index, length);
    }

    public ByteBuffer putFloat(int index, float value) {
        return buffer.putFloat(index, value);
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

    public ByteBuffer put(int index, byte[] src) {
        return buffer.put(index, src);
    }

    public ByteBuffer put(int index, byte b) {
        return buffer.put(index, b);
    }

    public ByteBuffer putLong(long value) {
        return buffer.putLong(value);
    }

    public double getDouble() {
        return buffer.getDouble();
    }

    public ByteBuffer putShort(int index, short value) {
        return buffer.putShort(index, value);
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

    public ByteBuffer putDouble(int index, double value) {
        return buffer.putDouble(index, value);
    }

    public int getInt() {
        return buffer.getInt();
    }

    public ByteBuffer put(byte b) {
        return buffer.put(b);
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

    public ByteBuffer putFloat(float value) {
        return buffer.putFloat(value);
    }

    public ByteBuffer asReadOnlyBuffer() {
        return buffer.asReadOnlyBuffer();
    }

    public ByteBuffer position(int newPosition) {
        return buffer.position(newPosition);
    }

    public ByteBuffer putChar(char value) {
        return buffer.putChar(value);
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

    public ByteBuffer put(int index, byte[] src, int offset, int length) {
        return buffer.put(index, src, offset, length);
    }

    public ByteBuffer putInt(int index, int value) {
        return buffer.putInt(index, value);
    }

    public ByteBuffer slice() {
        return buffer.slice();
    }

    @Override
    public int size() {
        return capacity;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public Buffer getBuffer() {
        return null;
    }

    public void reset() {

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

    public ByteBuffer put(ByteBuffer src) {
        return buffer.put(src);
    }

    public ByteBuffer flip() {
        return buffer.flip();
    }

    public ByteBuffer putDouble(double value) {
        return buffer.putDouble(value);
    }

    public ByteBuffer putShort(short value) {
        return buffer.putShort(value);
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

    public ByteBuffer put(int index, ByteBuffer src, int offset, int length) {
        return buffer.put(index, src, offset, length);
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

    public ByteBuffer putLong(int index, long value) {
        return buffer.putLong(index, value);
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

    public ByteBuffer put(byte[] src, int offset, int length) {
        return buffer.put(src, offset, length);
    }

    public int arrayOffset() {
        return buffer.arrayOffset();
    }

    public char getChar(int index) {
        return buffer.getChar(index);
    }

    public ByteBuffer putInt(int value) {
        return buffer.putInt(value);
    }
}
