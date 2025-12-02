package org.infinitytwo.nyctotile.block;

import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.model.builder.CubeModelBuilder;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.world.GridMap;

public class BedrockBlockType extends BlockType {
    public BedrockBlockType(int index) {
        super("bedrock", false, "bedrock",index);


    }

    @Override
    public void buildModel(GridMap map, int x, int y, int z, TextureAtlas atlas, BlockRegistry registry, NFloatBuffer buffer, RGBA light) {
        CubeModelBuilder.standardVerticesList(map,x,y,z,atlas.getUVCoords(textureIndex), light, buffer);
    }
}
