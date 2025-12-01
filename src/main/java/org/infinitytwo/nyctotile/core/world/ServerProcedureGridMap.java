package org.infinitytwo.nyctotile.core.world;

import org.infinitytwo.nyctotile.core.data.world.Block;
import org.infinitytwo.nyctotile.core.data.world.ChunkPos;
import org.infinitytwo.nyctotile.core.manager.WorkerThreads;
import org.infinitytwo.nyctotile.core.data.world.ChunkData;
import org.infinitytwo.nyctotile.core.exception.IllegalChunkAccessException;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.world.generation.Biome;
import org.infinitytwo.nyctotile.core.world.generation.CaveWorm;
import org.infinitytwo.nyctotile.core.world.generation.NoiseGenerationSettings;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import personthecat.fastnoise.FastNoise;

import java.util.*;
import java.util.concurrent.*;

public class ServerProcedureGridMap extends ServerGridMap {
    private final NoiseGenerationSettings dimension;
    protected long seed;
    private Random random;
    private final ConcurrentMap<ChunkPos, Future<ChunkData>> activeGenerations = new ConcurrentHashMap<>();
    protected Logger logger = LoggerFactory.getLogger(ServerProcedureGridMap.class);
    protected WorkerThreads threads = new WorkerThreads(5);
    
    private List<BiomeWeight> findTopBiomes(float temperature, float humidity, Biome[] biomes) {
        if (random == null) random = new Random(seed);
        List<BiomeWeight> weighted = new ArrayList<>();

        for (Biome biome : biomes) {
            float distSq = distanceSquared(temperature, humidity, biome.getTemperature(), biome.getHumidity());
            float weight = 1.0f / (distSq + 0.001f);
            weighted.add(new BiomeWeight(biome, weight));
        }

        weighted.sort((a, b) -> Float.compare(b.weight, a.weight));
        return weighted.subList(0, Math.min(3, weighted.size()));
    }

    private float distanceSquared(float t1, float h1, float t2, float h2) {
        float dt = t1 - t2;
        float dh = h1 - h2;
        return dt * dt + dh * dh;
    }
    
    @Override
    public Block getBlock(int x, int y, int z) {
        Vector2i p = convertToChunkPosition(x, z);
        ChunkPos pos = new ChunkPos(p.x, p.y);
        
        // 1. NON-BLOCKING CHECK: Check the already loaded chunks map (inherited from ServerGridMap)
        ChunkData data = chunks.get(pos);
        
        if (data != null) {
            // Chunk is loaded and ready: return the block data.
            int id = data.getBlockId(x, y, z);
            if (id != 0) return new Block(registry.get(id));
            return null; // Block is air (ID 0)
        }
        
        // 2. NON-BLOCKING CHECK: If the chunk is currently generating (or not generating at all),
        // we treat it as air for physics purposes to prevent blocking.
        // If it's generating, we rely on the Chunk Manager to load it soon.
        return null;
    }

    public void generateBiomeChunkHeightmap(NoiseGenerationSettings settings, ChunkPos GenChunk, Biome[] biomes) {
        final int seaLevel = settings.seaLevel;
        final int baseHeight = settings.baseHeight;
        FastNoise tempNoise = settings.temperature;
        FastNoise humidNoise = settings.humidity;
        FastNoise elevNoise = settings.elevation;
        FastNoise riverNoiseGen = settings.river;
        FastNoise holes = settings.holes;

        Map<ChunkPos, ChunkData> chunkDataMap = new HashMap<>();

        for (int x = GenChunk.x() * ChunkData.SIZE_X; x < (GenChunk.x() * ChunkData.SIZE_X) + ChunkData.SIZE_X; x++) {
            for (int z = GenChunk.z() * ChunkData.SIZE_X; z < GenChunk.z() * ChunkData.SIZE_Z +ChunkData.SIZE_X; z++) {
                ChunkData data = chunkDataMap.computeIfAbsent(GenChunk, ChunkData::new);

                float temperature = tempNoise.getNoise(x, z);
                float humidity = humidNoise.getNoise(x, z);
                float elevationNoise = elevNoise.getNoise(x, z);
                float riverNoise = riverNoiseGen.getNoise(x, z);

                List<BiomeWeight> topBiomes = findTopBiomes(temperature, humidity, biomes);

                float totalWeight = 0f;
                for (BiomeWeight bw : topBiomes) {
                    totalWeight += bw.weight;
                }
                for (BiomeWeight bw : topBiomes) {
                    bw.weight /= totalWeight;
                }

                float blendedElevation = 0f;
                float hilliness = 0f;
                float blendedTop = 0, blendedSoil = 0, blendedRocky = 0;

                for (BiomeWeight bw : topBiomes) {
                    Biome b = bw.biome;
                    float weight = bw.weight;
                    FastNoise detailNoise = b.getDetailNoise((int) seed + 45);
                    blendedElevation += (elevationNoise + detailNoise.getNoise(x, z)) * weight;

                    switch (b.getType()) {
                        case PLAINS -> hilliness += 3f * weight;
                        case HILL -> hilliness += 10f * weight;
                        case MOUNTAIN -> hilliness += 35f * weight;
                        case CANYON -> hilliness -= 25f * weight;
                        case RIVER, CORE -> hilliness += 0f;
                        default -> hilliness += 5f * weight;
                    }

                    blendedTop += b.getTop() * weight;
                    blendedSoil += b.getSoil() * weight;
                    blendedRocky += b.getRocky() * weight;
                }

                float riverBlend = Math.max(0, (0.10f - Math.abs(riverNoise)) / 0.10f);
                int biomeHeight = baseHeight + (int) (blendedElevation * hilliness);
                int height = (int) (biomeHeight * (1 - riverBlend) + (seaLevel - 2) * riverBlend);

                int localX = Math.floorMod(x, ChunkData.SIZE_X);
                int localZ = Math.floorMod(z, ChunkData.SIZE_Z);

                for (int y = 0; y < ChunkData.SIZE_Y; y++) {
                    try {
                        if (y > height) {
                            continue;
                        } else if (y == height) {
                            validate(Math.round(blendedTop));
                            data.setBlock(localX, y, localZ, Math.round(blendedTop));
                        } else if (y >= height - 3) {
                            validate(Math.round(blendedSoil));
                            data.setBlock(localX, y, localZ, Math.round(blendedSoil));
                        } else if (y == 0) {
                            validate(4);
                            data.setBlock(localX, y, localZ, 4);
                        } else {
                            validate(Math.round(blendedRocky));
                            data.setBlock(localX, y, localZ, Math.round(blendedRocky));
                        }
                    } catch (IllegalChunkAccessException e) {
                        throw new RuntimeException(e);
                    }

//                    try {
//                        List<CaveWorm> worms = generateCavePaths(GenChunk);
//                        for (CaveWorm worm : worms) {
//                            carveWormPath(data,worm);
//                        }
//                    } catch (IllegalChunkAccessExecption e) {
//                        new RuntimeException("Something went wrong while generation caves", e).printStackTrace();
//                    }
                }
                synchronized (chunks) {
                    if (!chunks.containsKey(GenChunk)) {
                        chunks.put(GenChunk,data);
                    }
                }
            }
        }
    }

    private void validate(int id) { // Only used for debugging
        if (id == 0) return;
        try {
            registry.get(id);
        } catch (Exception e) {
            WorkerThreads.dispatch(() -> {throw new RuntimeException(e);});
        }
    }

    private void carveWormPath(ChunkData data, CaveWorm worm) throws IllegalChunkAccessException {
        int steps = 80 + random.nextInt(60); // random tunnel length (80–140)
        float baseRadius = worm.radius;

        for (int i = 0; i < steps; i++) {
            worm.step();

            int cy = (int) Math.floor(worm.y);
            if (cy <= 1 || cy >= ChunkData.SIZE_Y - 1) continue;

            // Smoothly vary radius along the path
            float t = (float) i / steps;
            float radius = baseRadius * (0.75f + 0.5f * (float) Math.sin(t * Math.PI));

            worm.carve(
                    data,
                    steps
            );
        }
    }

    private List<CaveWorm> generateCavePaths(ChunkPos chunk) {
        List<CaveWorm> worms = new ArrayList<>();

        // maybe 0–3 worms per chunk
        int wormCount = random.nextInt(1,3);
        for (int i = 0; i < wormCount; i++) {
            double startX = chunk.x() * ChunkData.SIZE_X + random.nextInt(ChunkData.SIZE_X);
            double startZ = chunk.z() * ChunkData.SIZE_Z + random.nextInt(ChunkData.SIZE_Z);
            double startY = 20 + random.nextInt(40); // safe above bedrock, below surface

            CaveWorm worm = new CaveWorm(startX, startY, startZ, random);
            worms.add(worm);
        }
        return worms;
    }
    
    public void generate(ChunkPos chunk) {
        if (chunks.containsKey(chunk) || activeGenerations.containsKey(chunk)) return;
        
        // 2. Submit the task and store the future
        Future<ChunkData> future = threads.run(() -> {
            generateBiomeChunkHeightmap(
                    dimension,
                    chunk,
                    dimension.biomes
            );
            // The generateBiomeChunkHeightmap method MUST put the chunk into the 'chunks' map
            return chunks.get(chunk);
        });
        
        // 3. Store the Future for tracking
        activeGenerations.put(chunk, future);
    }
    
    public boolean isChunkLoadedOrGenerating(ChunkPos p) {
        // Check if it's already generated
        if (chunks.containsKey(p)) {
            return true;
        }
        // Check if it's currently being generated
        return activeGenerations.containsKey(p);
    }
    
    public ChunkData getChunkOrGenerate(Vector2i pos) {
        ChunkPos p = new ChunkPos(pos.x, pos.y);
        
        // 1. Check if the chunk is already generated and ready
        if (chunks.containsKey(p)) {
            return chunks.get(p);
        }
        
        // 2. Check if the chunk is already generating
        Future<ChunkData> future = activeGenerations.get(p);
        
        if (future == null) {
            // 3. Not generating, so submit a new generation task and get the future
            // NOTE: This logic ensures only ONE thread creates the Future for this chunk.
            future = activeGenerations.computeIfAbsent(p, k -> threads.run(() -> {
                generateBiomeChunkHeightmap(
                        dimension,
                        k,
                        dimension.biomes
                );
                return chunks.get(k); // Assumes generateBiomeChunkHeightmap puts it in 'chunks'
            }));
        }
        
        try {
            // 4. BLOCK UNTIL THE CHUNK IS READY
            ChunkData data = future.get();
            
            // 5. Cleanup: Generation is complete, remove the Future
            activeGenerations.remove(p);
            
            return data;
        } catch (ExecutionException | InterruptedException e) {
            // Handle failure, ensure the Future is removed if it failed
            activeGenerations.remove(p);
            throw new RuntimeException("Chunk generation failed or was interrupted", e);
        }
    }
    
    public void generate(int x, int y) {
        generate(new ChunkPos(x,y));
    }
    
    public ChunkData getChunkOrGenerate(int x, int y) {
        return getChunkOrGenerate(new Vector2i(x,y));
    }
    
    private static class BiomeWeight {
        Biome biome;
        float weight;

        BiomeWeight(Biome biome, float weight) {
            this.biome = biome;
            this.weight = weight;
        }
    }

    public ServerProcedureGridMap(NoiseGenerationSettings dimension, BlockRegistry registry) {
        super(registry);
        this.dimension = dimension;
        this.random = new Random(seed+58);
    }
}
