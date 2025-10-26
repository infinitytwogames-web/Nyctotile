package org.infinitytwo.umbralore.entity;

import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.Inventory;
import org.infinitytwo.umbralore.core.data.buffer.NFloatBuffer;
import org.infinitytwo.umbralore.core.entity.Entity;
import org.infinitytwo.umbralore.core.model.Model;
import org.infinitytwo.umbralore.core.model.builder.ModelBuilder;
import org.infinitytwo.umbralore.core.registry.ModelRegistry;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;

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
}
