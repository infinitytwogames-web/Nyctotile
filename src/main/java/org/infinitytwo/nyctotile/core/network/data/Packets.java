package org.infinitytwo.nyctotile.core.network.data;

import org.infinitytwo.nyctotile.core.data.world.ChunkData;
import org.infinitytwo.nyctotile.core.data.io.DataSchematica;
import org.infinitytwo.nyctotile.core.data.io.DataSchematica.Data;
import org.infinitytwo.nyctotile.core.entity.Entity;
import org.infinitytwo.nyctotile.core.entity.Player;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Packets {
    
    static {
        register();
    }
    
    public static void register() {
        DataSchematica.register(new PCommandData("", new byte[0]));
        DataSchematica.register(new PChunk(0, 0, new int[0]));
        DataSchematica.register(new PCommand(""));
        DataSchematica.register(new PPosition(0,0,0,0,0));
        DataSchematica.register(new Failure(""));
    }
    
    public record PChunk(int x, int y, int[] blocks) implements Data {
        @Override
        public byte[] serialize() {
            ByteBuffer buffer = ByteBuffer.allocate((2 + blocks.length) * Integer.BYTES);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(x);
            buffer.putInt(y);
            for (int b : blocks) buffer.putInt(b);
            return buffer.array();
        }
        
        @Override
        public PChunk deserialize(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int x, y;
            x = buffer.getInt();
            y = buffer.getInt();
            int[] blocks = new int[ChunkData.SIZE * ChunkData.SIZE * ChunkData.SIZE_Y];
            for (int i = 0; i < blocks.length; i++) blocks[i] = buffer.getInt();
            return new PChunk(x, y, blocks);
        }
    }
    
    public record PCommand(String command) implements Data {
        
        @Override
        public byte[] serialize() {
            return command.getBytes(StandardCharsets.UTF_8);
        }
        
        @Override
        public Data deserialize(byte[] data) {
            return new PCommand(new String(data, StandardCharsets.UTF_8));
        }
    }
    
    public record PCommandData(String command, byte[] data) implements Data {
        @Override
        public byte[] serialize() {
            byte[] cmd = command.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + cmd.length + data.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(cmd.length);
            buffer.put(cmd);
            buffer.put(data);
            return buffer.array();
        }
        
        
        @Override
        public PCommandData deserialize(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            int l = buffer.getInt();
            byte[] cmd = new byte[l];
            buffer.get(cmd);
            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);
            
            return new PCommandData(new String(cmd, StandardCharsets.UTF_8), payload);
        }
    }
    
    public record PPosition(long leastSignificant, long mostSignificant, float x, float y, float z) implements Data {
        
        // Size: 2 Longs (UUID) + 3 Floats (X, Y, Z) = 28 bytes
        private static final int PACKET_SIZE = (2 * Long.BYTES) + (3 * Float.BYTES);
        
        public PPosition(Entity player) {
            this(player.getUUID().getLeastSignificantBits(),player.getUUID().getMostSignificantBits(),player.getPosition().x,player.getPosition().y,player.getPosition().z);
        }
        
        public PPosition(Player player) {
            this(0,0,player.getPosition().x,player.getPosition().y,player.getPosition().z);
        }
        
        @Override
        public byte[] serialize() {
            ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // 1. Write UUID: IMPORTANT: Write MOST Significant first for consistency
            buffer.putLong(mostSignificant);
            buffer.putLong(leastSignificant);
            
            // 2. Write Position
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(z);
            
            return buffer.array();
        }
        
        @Override
        public PPosition deserialize(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // 1. Read UUID in the same order as it was written
            long mostSigBits = buffer.getLong();
            long leastSigBits = buffer.getLong();
            
            // 2. Read Position
            float x = buffer.getFloat();
            float y = buffer.getFloat();
            float z = buffer.getFloat();
            
            // CRITICAL FIX: Use the new read 'mostSigBits' and 'leastSigBits'
            // to construct the returned object, not the fields of the registration object.
            return new PPosition(leastSigBits, mostSigBits, x, y, z);
        }
    }
    
    public static class Failure implements Data {
        public final String msg;
        
        public Failure(String string) {
            msg = string;
        }
        
        @Override
        public byte[] serialize() {
            return msg.getBytes(StandardCharsets.UTF_8);
        }
        
        @Override
        public Data deserialize(byte[] data) {
            return new Failure(new String(data,StandardCharsets.UTF_8));
        }
    }
}
