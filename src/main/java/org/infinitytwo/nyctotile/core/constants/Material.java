package org.infinitytwo.nyctotile.core.constants;

public enum Material {
    GRASS("grass"),
    STONE("stone"),
    DIRT("dirt"),
    BEDROCK("bedrock")
    ;

    public final String material;
    Material(String name) {
        material = name;
    }
}
