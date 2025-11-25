package org.infinitytwo.nyctotile.core.constants;

public enum NoisePresets {
    DESERT(1,0.35f,0.007f),
    PLAINS(1,0.35f, 0.007f),
    MOUNTAINS(2, 0.52f, 0.05f),
    EXTREME_MOUNTAINS(2, 10, 0.075f),
    OCEAN(1,0.0025f,0.0025f),
    MESA(3,0.35f,0.007f),
    RIVER(1,0.01f,0.0025f),
    ;

    public final int octaves;
    public final float gain;
    public final float frequency;

    NoisePresets(int octaves, float gain, float frequency) {
        this.octaves = octaves;
        this.gain = gain;
        this.frequency = frequency;
    }
}
