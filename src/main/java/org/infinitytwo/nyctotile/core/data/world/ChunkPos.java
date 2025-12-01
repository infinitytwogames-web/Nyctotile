package org.infinitytwo.nyctotile.core.data.world;

import org.infinitytwo.nyctotile.core.renderer.Chunk;
import org.joml.Vector2i;

public record ChunkPos(int x, int z) {
    public ChunkPos(Chunk chunk) {
        this(chunk.getPosition().x,chunk.getPosition().y);
    }
    
    public ChunkPos(Vector2i pos) {
        this(pos.x,pos.y);
    }
}
