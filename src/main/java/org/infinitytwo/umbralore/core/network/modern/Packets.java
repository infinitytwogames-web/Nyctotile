package org.infinitytwo.umbralore.core.network.modern;

import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.data.io.DataSchematica;
import org.infinitytwo.umbralore.core.data.io.DataSchematica.Data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Packets {
    
    static {
        register();
    }
    
    public static void register() {
        DataSchematica.register(new PCommandData("",new byte[0]));
        DataSchematica.register(new PChunk(0,0,new int[0]));
        DataSchematica.register(new PCommand(""));
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
        public PChunk unserialize(byte[] data) {
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
        public Data unserialize(byte[] data) {
            return new PCommand(new String(data,StandardCharsets.UTF_8));
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
        public PCommandData unserialize(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            int l = buffer.getInt();
            byte[] cmd = new byte[l];
            buffer.get(cmd);
            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);
            
            return new PCommandData(new String(cmd,StandardCharsets.UTF_8),payload);
        }
    }
}
