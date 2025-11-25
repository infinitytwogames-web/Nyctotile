package org.infinitytwo.nyctotile.entity;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.Inventory;
import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;
import org.infinitytwo.nyctotile.core.entity.Entity;
import org.infinitytwo.nyctotile.core.model.Model;
import org.infinitytwo.nyctotile.core.model.builder.ModelBuilder;
import org.infinitytwo.nyctotile.core.registry.ModelRegistry;
import org.infinitytwo.nyctotile.core.world.dimension.Dimension;

public class ItemEntity extends Entity {
    private static final int index;

    static {
        Model model = new Model("item");
        NFloatBuffer buffer = new NFloatBuffer();
        ModelBuilder b = new ModelBuilder(0,0,0,0.5f,0.5f,0.5f);
        b.cube(buffer,new float[]{0,0,1,1});

        model.setVertices(buffer.getBuffer());
        index = ModelRegistry.register(model);

        buffer.cleanup();
    }

    protected ItemEntity(Dimension map, Window window) {
        super("item", map, window, new Inventory(1));

        setModelIndex(index);
    }

    @Override
    public Entity newInstance() {
        return new ItemEntity(null,null);
    }
}
