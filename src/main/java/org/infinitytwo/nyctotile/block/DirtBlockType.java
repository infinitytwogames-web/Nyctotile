package org.infinitytwo.nyctotile.block;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.model.builder.CubeModelBuilder;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.world.GridMap;

public class DirtBlockType extends BlockType {
    public DirtBlockType(int index) {
        super("soil",false,"dirt",index);
    }

    @Override
    public void buildModel(GridMap map, int x, int y, int z, TextureAtlas atlas, BlockRegistry registry, NFloatBuffer b, RGBA light) {
        CubeModelBuilder.standardVerticesList(map,x,y,z,atlas.getUVCoords(textureIndex), light, b);
    }
}
