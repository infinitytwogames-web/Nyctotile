package org.infinitytwo.nyctotile.core.data.io;

import org.infinitytwo.nyctotile.core.VectorMath;
import org.infinitytwo.nyctotile.core.entity.Entity;
import org.infinitytwo.nyctotile.core.registry.DimensionRegistry;
import org.infinitytwo.nyctotile.core.registry.EntityRegistry;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.UUID;

public class EntitySerializer {
    public static byte[] serialize(Entity entity) {
        ByteBuffer buffer = ByteBuffer.allocate((18 * Float.BYTES) + (2 * Integer.BYTES) + (2 * Long.BYTES));

        buffer.putInt(EntityRegistry.getRegistry().getId(entity.getId())); // int
        buffer.putLong(entity.getUUID().getMostSignificantBits()); // long
        buffer.putLong(entity.getUUID().getLeastSignificantBits()); // long
        buffer.putInt(DimensionRegistry.getRegistry().getId(entity.getDimension().getName())); // int
        buffer.put(VectorMath.serialize(entity.getPosition())); // 3 float
        buffer.put(VectorMath.serialize(entity.getVelocity())); // 3 float
        buffer.put(VectorMath.serialize(entity.getRotation())); // 3 float
        buffer.put(VectorMath.serialize(entity.getScale())); // 3 float
        buffer.putFloat(entity.getGravity()); // float
        buffer.putFloat(entity.getMovementSpeed()); // float
        buffer.putFloat(entity.getJumpStrength()); // float

        return buffer.array();
    }

    public static Data unserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);

        buffer.put(data);
        return unserialize(buffer);
    }

    public static Data unserialize(ByteBuffer buffer) {
        buffer.position(0);

        return new Data(
                // Entity ID (int)
                buffer.getInt(),
                // UUID (2 longs)
                new UUID(buffer.getLong(),buffer.getLong()),
                // Dimension ID (int)
                buffer.getInt(),
                // Position (3 floats)
                new Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat()),
                // Velocity (3 floats)
                new Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat()),
                // Rotation (3 floats)
                new Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat()),
                // Scale (3 floats)
                new Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat()),
                // Gravity (float)
                buffer.getFloat(),
                // Speed (float)
                buffer.getFloat(),
                // Jump Strength (float)
                buffer.getFloat()
        );
    }

    public record Data(int id, UUID uuid, int dimension, Vector3f position, Vector3f velocity,
                       Vector3f rotation, Vector3f scale, float gravity, float speed,
                       float jump
    ) {
        public void apply(Entity entity) {
            entity.setPosition(position);
            entity.setVelocity(velocity);
            entity.setRotation(rotation);
            entity.setScale(scale);
            entity.setGravity(gravity);
            entity.setMovementSpeed(speed);
            entity.setJumpStrength(jump);
        }
    }
}
