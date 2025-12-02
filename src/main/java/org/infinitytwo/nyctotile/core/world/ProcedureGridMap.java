package org.infinitytwo.nyctotile.core.world;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.world.ChunkData;
import org.infinitytwo.nyctotile.core.data.world.ChunkPos;
import org.infinitytwo.nyctotile.core.exception.IllegalChunkAccessException;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.renderer.Camera;
import org.infinitytwo.nyctotile.core.renderer.Chunk;
import org.infinitytwo.nyctotile.core.renderer.ShaderProgram;
import org.infinitytwo.nyctotile.core.world.generation.Biome;
import org.infinitytwo.nyctotile.core.world.generation.NoiseGenerationSettings;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import personthecat.fastnoise.FastNoise;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public class ProcedureGridMap extends GridMap {
    private final NoiseGenerationSettings dimension;
    protected long seed;
    private Random random;
    protected Logger logger = LoggerFactory.getLogger(ProcedureGridMap.class);
    protected static AtomicInteger generationLimit = new AtomicInteger(8);
    protected static AtomicInteger current = new AtomicInteger(0);
    protected final List<ChunkData> chunkData = Collections.synchronizedList(new ArrayList<>());
    protected ShaderProgram program;
    protected TextureAtlas atlas;
    protected ExecutorService generationThreads;
    protected ChunkData nextData = null;
    protected final ConcurrentLinkedQueue<ChunkPos> processChunks = new ConcurrentLinkedQueue<>();
    
    private List<ProcedureGridMap.BiomeWeight> findTopBiomes(float temperature, float humidity, Biome[] biomes) {
        if (random == null) random = new Random(seed);
        List<ProcedureGridMap.BiomeWeight> weighted = new ArrayList<>();
        
        for (Biome biome : biomes) {
            float distSq = distanceSquared(temperature, humidity, biome.getTemperature(), biome.getHumidity());
            float weight = 1.0f / (distSq + 0.001f);
            weighted.add(new ProcedureGridMap.BiomeWeight(biome, weight));
        }
        
        weighted.sort((a, b) -> Float.compare(b.weight, a.weight));
        return weighted.subList(0, Math.min(3, weighted.size()));
    }
    
    private float distanceSquared(float t1, float h1, float t2, float h2) {
        float dt = t1 - t2;
        float dh = h1 - h2;
        return dt * dt + dh * dh;
    }
    
    public void generateBiomeChunkHeightmap(NoiseGenerationSettings settings, ChunkPos GenChunk, Biome[] biomes, ShaderProgram program, TextureAtlas atlas) {
        if (generationLimit.get() <= current.get() + 1) {
            logger.error("GENERATION SUCCEEDS THREAD LIMIT! SKIPPING!");
            return;
        }
        
        current.incrementAndGet();
        final int seaLevel = settings.seaLevel;
        final int baseHeight = settings.baseHeight;
        FastNoise tempNoise = settings.temperature;
        FastNoise humidNoise = settings.humidity;
        FastNoise elevNoise = settings.elevation;
        FastNoise riverNoiseGen = settings.river;
        Map<ChunkPos, ChunkData> chunkDataMap = new HashMap<>();
        
        for (int x = GenChunk.x() * Chunk.SIZE_X; x < (GenChunk.x() * Chunk.SIZE_X) + Chunk.SIZE_X; x++) {
            for (int z = GenChunk.z() * Chunk.SIZE_X; z < GenChunk.z() * Chunk.SIZE_Z + Chunk.SIZE_X; z++) {
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
                
                int localX = Math.floorMod(x, Chunk.SIZE_X);
                int localZ = Math.floorMod(z, Chunk.SIZE_Z);
                
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    try {
                        if (y > height) {
                            continue;
                        } else if (y == height) {
                            data.setBlock(localX, y, localZ, (short) Math.round(blendedTop));
                        } else if (y >= height - 3) {
                            data.setBlock(localX, y, localZ, (short) Math.round(blendedSoil));
                        } else if (y == 0) {
                            data.setBlock(localX, y, localZ, (short) 4);
                        } else {
                            data.setBlock(localX, y, localZ, (short) Math.round(blendedRocky));
                        }
                    } catch (IllegalChunkAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                synchronized (chunks) {
                    if (!chunks.containsKey(GenChunk)) {
                        chunkData.add(data);
                    }
                }
                
            }
        }
        
        current.decrementAndGet();
        logger.info("Generated x:" + GenChunk.x() * 16 + " y:" + GenChunk.z() * 16);
    }
    
    public void generate(ChunkPos chunk) {
        if (!processChunks.contains(chunk)) {
            processChunks.add(chunk);
            generationThreads.submit(() -> {
                generateBiomeChunkHeightmap(
                        dimension,
                        chunk,
                        dimension.biomes,
                        program,
                        atlas
                );
                processChunks.remove(chunk);
            });
        }
    }
    
    private static class BiomeWeight {
        Biome biome;
        float weight;
        
        BiomeWeight(Biome biome, float weight) {
            this.biome = biome;
            this.weight = weight;
        }
    }
    
    public ProcedureGridMap(ShaderProgram program, TextureAtlas atlas, int generatorThreads, NoiseGenerationSettings dimension, BlockRegistry registry) {
        super(registry);
        this.program = program;
        this.atlas = atlas;
        this.dimension = dimension;
        generationThreads = Executors.newFixedThreadPool(generatorThreads);
    }
    
    public void update() {
        logger.info(String.valueOf(current.get()));
        if (current.get() == 0) {
            if (!chunkData.isEmpty()) {
                logger.info("Helo");
                for (Chunk chunk : chunks.values()) {
                    if (chunk.isDirty()) {
                        chunk.rebuild();
                        break;
                    }
                }
            }
            synchronized (chunkData) {
                
                if (!chunkData.isEmpty()) {
                    nextData = chunkData.remove(0);
                }
            }
            if (nextData != null) {
                addChunk(nextData.createChunk(atlas, registry));
            }
            
        }
    }
    
    @Override
    public void draw(Camera camera, Window window, int view) {
        update();
        Vector2i chunkP = convertToChunkPosition((int) camera.getPosition().x, (int) camera.getPosition().z);
        ChunkPos pos = new ChunkPos(chunkP.x, chunkP.y);
        List<ChunkPos> r = getSurroundingChunks(pos, view);
        for (ChunkPos search : r) {
            if (chunks.containsKey(search)) {
                chunks.get(search).draw(camera, window);
            } else {
                if (!processChunks.contains(search)) {
                    processChunks.add(search);
                    generationThreads.submit(() -> {
                        generateBiomeChunkHeightmap(
                                dimension,
                                search,
                                dimension.biomes,
                                program,
                                atlas
                        );
                        processChunks.remove(search);
                    });
                }
            }
        }
    }
}
