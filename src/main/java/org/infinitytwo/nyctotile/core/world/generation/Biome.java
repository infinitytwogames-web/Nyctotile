package org.infinitytwo.nyctotile.core.world.generation;

import org.infinitytwo.nyctotile.core.constants.TerrainGenerationType;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.NoiseType;

public abstract class Biome {
    protected final String name;
    protected final String id;
    protected final float humidity;
    protected final float temperature;
    protected final int top;
    protected final int soil;
    protected final int rocky;
    protected final TerrainGenerationType type;

    public float getBlendWeight(float temp, float humidity) {
        float dt = this.temperature - temp;
        float dh = this.humidity - humidity;
        float distSquared = dt * dt + dh * dh;
        return 1f / (distSquared + 0.001f); // Prevent division by zero
    }

    protected Biome(String name, String id, float humidity, float temperature, int top, int soil, int rocky, TerrainGenerationType type) {
        this.name = name;
        this.id = id;
        this.humidity = humidity;
        this.temperature = temperature;
        this.top = top;
        this.soil = soil;
        this.rocky = rocky;
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public String getId() {
        return id;
    }
    public float getHumidity() {
        return humidity;
    }
    public float getTemperature() {
        return temperature;
    }

    public FastNoise getDetailNoise(int seed) {
         return FastNoise.builder()
                .octaves(2)
                .seed(seed + 6)
                .type(NoiseType.SIMPLEX)
                .frequency(0.0154f)
                .build();
    }

    public TerrainGenerationType getType() {
        return type;
    }

    public int getTop() {
        return top;
    }

    public int getSoil() {
        return soil;
    }

    public int getRocky() {
        return rocky;
    }
}
