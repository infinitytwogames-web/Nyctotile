package org.infinitytwo.umbralore.core.world.dimension;

import org.infinitytwo.umbralore.core.constants.Biomes;
import org.infinitytwo.umbralore.core.context.ServerContext;
import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.data.PlayerData;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
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
                }), new ServerProcedureGridMap(new NoiseGenerationSettings(
                        62,64,seed, new Biome[]{
                        Biomes.PLAINS.biome,
                        Biomes.DESERT.biome,
                        Biomes.MOUNTAINS.biome,
                }),registry),
                new ArrayList<>());
    }
    
    @Override
    public void generate(int x, int y) {
        ServerProcedureGridMap world = this.world;
        world.generate(x,y);
    }
    
    public ChunkData generateSync(int x, int y) {
        ServerProcedureGridMap world = this.world;
        return world.getChunkOrGenerate(x,y);
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
}
