package org.infinitytwo.umbralore.core.data;

import org.infinitytwo.umbralore.core.renderer.Chunk;

public record ChunkPos(int x, int z) {
    public ChunkPos(Chunk chunk) {
        this(chunk.getPosition().x,chunk.getPosition().y);
    }
}
