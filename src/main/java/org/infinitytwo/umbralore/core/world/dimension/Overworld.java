package org.infinitytwo.umbralore.core.world.dimension;

import org.infinitytwo.umbralore.core.constants.Biomes;
import org.infinitytwo.umbralore.core.context.ClientContext;
import org.infinitytwo.umbralore.core.context.ServerContext;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.data.PlayerData;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.world.ServerGridMap;
import org.infinitytwo.umbralore.core.world.ServerProcedureGridMap;
import org.infinitytwo.umbralore.core.world.generation.Biome;
import org.infinitytwo.umbralore.core.world.generation.NoiseGenerationSettings;

import java.util.ArrayList;

public class Overworld extends Dimension {
    public Overworld(int seed, BlockRegistry registry) {
        super("Overworld", "overworld",
                new NoiseGenerationSettings(
                        62,64,seed, new Biome[]{
                        Biomes.PLAINS.biome,
                        Biomes.DESERT.biome,
                        Biomes.MOUNTAINS.biome,
                }), new ServerProcedureGridMap(4,new NoiseGenerationSettings(
                        62,64,seed, new Biome[]{
                        Biomes.PLAINS.biome,
                        Biomes.DESERT.biome,
                        Biomes.MOUNTAINS.biome,
                }),registry),
                new ArrayList<>());
    }

    @Override
    public void generate(ChunkPos chunk) {
        world.generate(chunk);
    }


    @Override
    public void playerEntered(ServerContext context, PlayerData playerData) {

    }

    @Override
    public void playerLeave(PlayerData playerData) {

    }

    @Override
    public void tick(ServerContext context) {

    }

    @Override
    public void draw(ClientContext context) {

    }
}
