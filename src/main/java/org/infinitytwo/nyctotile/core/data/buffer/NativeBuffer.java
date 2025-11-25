package org.infinitytwo.nyctotile.core.data.buffer;

import java.nio.Buffer;
import java.nio.ByteOrder;

public abstract class NativeBuffer implements AutoCloseable {
    protected int capacity;
    protected int written;
    protected static final int INITIAL = 4096;

    public int size() { return written; }
    public int capacity() { return capacity; }

    public abstract void cleanup();
    public abstract Buffer getBuffer();
    public abstract void reset();
    public abstract Buffer duplicate();
    public abstract Buffer limit(int newLimit);
    public abstract Buffer asReadOnlyBuffer();
    public abstract Buffer position(int newPosition);
    public abstract Buffer mark();
    public abstract int position();
    public abstract int remaining();
    public abstract Buffer flip();
    public abstract ByteOrder order();
    public abstract Buffer compact();
    public abstract Buffer rewind();
    public abstract boolean isDirect();
    public abstract boolean isReadOnly();
    public abstract Buffer slice();
    public abstract boolean hasRemaining();
    public abstract boolean hasArray();
    public abstract Buffer slice(int index, int length);
    public abstract int limit();
    public abstract int arrayOffset();

    @Override
    public void close() {
        cleanup();
    }

    public int getWritten() { return written; }
}
