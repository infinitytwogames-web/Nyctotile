package org.infinitytwo.nyctotile.core.world.dimension;

import org.infinitytwo.nyctotile.core.constants.Biomes;
import org.infinitytwo.nyctotile.core.context.ServerContext;
import org.infinitytwo.nyctotile.core.data.world.ChunkData;
import org.infinitytwo.nyctotile.core.data.PlayerData;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.world.ServerProcedureGridMap;
import org.infinitytwo.nyctotile.core.world.generation.Biome;
import org.infinitytwo.nyctotile.core.world.generation.NoiseGenerationSettings;

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
