package org.infinitytwo.umbralore.world;

import org.infinitytwo.umbralore.data.ChunkData;
import org.infinitytwo.umbralore.exception.IllegalChunkAccessExecption;
import org.infinitytwo.umbralore.logging.Logger;
import org.infinitytwo.umbralore.registry.BlockRegistry;
import org.infinitytwo.umbralore.world.generation.Biome;
import org.infinitytwo.umbralore.world.generation.CaveWorm;
import org.infinitytwo.umbralore.world.generation.NoiseGenerationSettings;
import org.joml.Vector2i;
import personthecat.fastnoise.FastNoise;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerProcedureGridMap extends ServerGridMap {
    private final NoiseGenerationSettings dimension;
    protected long seed;
    private Random random;
    protected Logger logger = new Logger("Procedure GridMap Generator");
    protected static AtomicInteger generationLimit = new AtomicInteger(8);
    protected static AtomicInteger current = new AtomicInteger(0);
    protected final List<ChunkData> chunkData = Collections.synchronizedList(new ArrayList<>());
    protected ExecutorService generationThreads;
    protected ChunkData nextData = null;
    protected final ConcurrentLinkedQueue<ChunkPos> processChunks = new ConcurrentLinkedQueue<>();

    public void cleanup() {
        generationThreads.shutdown();
    }

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

    public void generateBiomeChunkHeightmap(NoiseGenerationSettings settings, ChunkPos GenChunk, Biome[] biomes) {
        if (generationLimit.get() <= current.get()+1) {
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
                            data.setBlock(localX, y, localZ, (short) Math.round(blendedTop));
                        } else if (y >= height - 3) {
                            data.setBlock(localX, y, localZ, (short) Math.round(blendedSoil));
                        } else if (y == 0) {
                            data.setBlock(localX, y, localZ, (short) 4);
                        } else {
                            data.setBlock(localX, y, localZ, (short) Math.round(blendedRocky));
                        }
                    } catch (IllegalChunkAccessExecption e) {
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
        current.decrementAndGet();
        logger.info("Generated x:"+GenChunk.x() * 16+" y:"+GenChunk.z() *16);
    }

    private void carveWormPath(ChunkData data, CaveWorm worm) throws IllegalChunkAccessExecption {
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
        if (!processChunks.contains(chunk)) {
            processChunks.add(chunk);
            generationThreads.submit(() -> {
                generateBiomeChunkHeightmap(
                        dimension,
                        chunk,
                        dimension.biomes
                );
                processChunks.remove(chunk);
            });
        }
    }

    public ChunkData getChunkOrGenerate(Vector2i pos) {
        ChunkPos p = new ChunkPos(pos.x, pos.y);

        // Check if the chunk is already being processed or has been generated
        if (chunks.containsKey(p)) {
            return chunks.get(p);
        }
        if (processChunks.contains(p)) {
            // The chunk is already being processed, wait for it to be added to the chunks map
            while (!chunks.containsKey(p)) {
                try {
                    // Sleep for a short period to prevent a busy loop
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return chunks.get(p);
        }

        // If not, submit a new generation task and block until it's done
        try {
            Future<ChunkData> future = generationThreads.submit(() -> {
                generateBiomeChunkHeightmap(
                        dimension,
                        p,
                        dimension.biomes
                );
                return chunks.get(p);
            });

            // Wait for the task to complete and return the generated chunk data
            return future.get(); // This will block until the task is done
        } catch (Exception e) {
            logger.error(e,"Failed to generate and retrieve chunk");

            return null;
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

    public ServerProcedureGridMap(int generatorThreads, NoiseGenerationSettings dimension, BlockRegistry registry) {
        super(registry);
        this.dimension = dimension;
        this.random = new Random(seed+58);
        generationThreads = Executors.newFixedThreadPool(generatorThreads);
    }


//    public ChunkData getChunkOrGenerate(Vector2i pos) {
//        try {
//            return getChunk(pos);
//        } catch (IllegalChunkAccessExecption e) {
//            generate(new ChunkPos(pos.x,pos.y));
//            return null;
//        }
//    }
}
