package org.infinitytwo.umbralore.block;

import org.infinitytwo.umbralore.core.data.AABB;
import org.infinitytwo.umbralore.core.data.buffer.NFloatBuffer;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.registry.Registerable;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.joml.Vector3i;

import java.util.List;

public abstract class BlockType implements Registerable {
    public int textureIndex;
    protected AABB[] hitboxes = {new AABB(0, 0, 0, 1, 1, 1)};
    public List<Float> vertex;
    protected final String material;
    protected final boolean invisible;
    protected final String id;
    protected boolean collidable = true;
    protected float friction = 1;

    public BlockType(String material, boolean invisible, String name, int textureIndex) {
        this.textureIndex = textureIndex;
        this.material = material;
        this.invisible = invisible;
        this.id = name;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public AABB[] getBoundingBox() {
        return hitboxes;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return id;
    }

    public String getId() {
        return id;
    }

    public static BlockType standard(String material, String name) {
        return new BlockType(material, false, name, 0) {
            @Override
            public void buildModel(GridMap gridMap, int x, int y, int z, TextureAtlas atlas, BlockRegistry registry, NFloatBuffer buffer) {
            }
        };
    }

    public void buildModel(GridMap map, Vector3i pos, TextureAtlas atlas, BlockRegistry registry, NFloatBuffer buffer) {
        buildModel(map, pos.x, pos.y, pos.z, atlas, registry, buffer);
    }

    public boolean isCollidable() {
        return collidable;
    }

    public AABB[] getBoundingBoxes() {
        return hitboxes;
    }

    public abstract void buildModel(GridMap gridMap, int x, int y, int z, TextureAtlas atlas, BlockRegistry registry, NFloatBuffer buffer);

    public float getFriction() {
        return friction;
    }
}
