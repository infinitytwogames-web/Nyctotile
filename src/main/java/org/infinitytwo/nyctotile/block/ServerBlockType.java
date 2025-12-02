package org.infinitytwo.nyctotile.block;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.world.GridMap;

public class ServerBlockType extends BlockType {
    public ServerBlockType(String material, boolean b, String name) {
        super(material, b, name, 0);
    }

    @Override
    public void buildModel(GridMap map, int x, int y, int z, TextureAtlas atlas, BlockRegistry registry, NFloatBuffer buffer, RGBA light) {

    }
}
