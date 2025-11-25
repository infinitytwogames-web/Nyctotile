package org.infinitytwo.nyctotile.core.world.generation;

import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.NoiseType;

public class NoiseGenerationSettings {
    public FastNoise holes;
    public FastNoise humidity;
    public FastNoise temperature;
    public FastNoise river;
    public FastNoise elevation;
    public FastNoise detail;
    public final int seaLevel;
    public final int baseHeight;
    public Biome[] biomes;

    public NoiseGenerationSettings(FastNoise cave, FastNoise humidity, FastNoise temperature, FastNoise river, FastNoise elevation, int seaLevel, int baseHeight, Biome[] biomes) {
        this.holes = cave;
        this.humidity = humidity;
        this.temperature = temperature;
        this.river = river;
        this.elevation = elevation;
        this.seaLevel = seaLevel;
        this.baseHeight = baseHeight;
        this.biomes = biomes;
    }

    public NoiseGenerationSettings(int seaLevel, int baseHeight, int seed, Biome[] biomes) {
        this.seaLevel = seaLevel;
        this.baseHeight = baseHeight;
        this.biomes = biomes;
        build(seed);
    }

    public NoiseGenerationSettings(int seaLevel, int baseHeight, Biome[] biomes) {
        this.seaLevel = seaLevel;
        this.baseHeight = baseHeight;
        this.biomes = biomes;
    }

    public void build(int seed) {
        temperature = FastNoise.builder()
                .type(NoiseType.PERLIN)
                .octaves(5)
                .frequency(0.0015f)
                .gain(0.6f)
                .lacunarity(2f)
                .seed(seed +3)
                .build();
        humidity = FastNoise.builder()
                .octaves(4)
                .frequency(0.05f)
                .gain(0.5f)
                .lacunarity(2f)
                .seed(seed)
                .build();
        elevation = FastNoise.builder()
                .type(NoiseType.PERLIN)
                .frequency(0.05f)
                .gain(0.7f)
                .seed(seed +6)
                .build();
        river = FastNoise.builder()
                .octaves(1)
                .frequency(0.0015f)
                .type(NoiseType.SIMPLEX)
                .seed(seed + 4)
                .build();
        holes = FastNoise.builder()
                .type(NoiseType.SIMPLEX)
                .octaves(2)
                .seed(seed + 99)
                .frequency(0.05f)
                .build();
    }

    public void buildUnset(int seed) {
        if (temperature == null) {
            temperature = FastNoise.builder()
                    .type(NoiseType.PERLIN)
                    .octaves(5)
                    .frequency(0.015f)
                    .gain(0.6f)
                    .lacunarity(2f)
                    .seed(seed)
                    .build();
        } if (humidity == null) {
            humidity = FastNoise.builder()
                    .octaves(4)
                    .frequency(0.05f)
                    .gain(0.5f)
                    .lacunarity(2f)
                    .seed(seed)
                    .build();
        } if (elevation == null) {
            elevation = FastNoise.builder()
                    .type(NoiseType.PERLIN)
                    .frequency(0.015f)
                    .gain(0.5f)
                    .seed(seed)
                    .build();
        } if (river == null) {
            river = FastNoise.builder()
                    .octaves(2)
                    .frequency(0.005f)
                    .type(NoiseType.PERLIN)
                    .seed(seed)
                    .build();
        } if (holes == null) {
            holes = FastNoise.builder()
                    .type(NoiseType.SIMPLEX)
                    .octaves(2)
                    .seed(seed + 99)
                    .frequency(0.05f)
                    .build();
        }
    }

    public static NoiseGenerationSettings getDefault(int seed, Biome[] biomes) {
        NoiseGenerationSettings settings = new NoiseGenerationSettings(58,64, biomes);
        settings.build(seed);
        return settings;
    }

    public boolean hasBuilt() {
        return temperature != null && humidity != null && elevation != null && river != null;
    }
}
