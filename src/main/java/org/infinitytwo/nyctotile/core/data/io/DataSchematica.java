package org.infinitytwo.nyctotile.core.data.io;

import org.infinitytwo.nyctotile.core.exception.UnknownRegistryException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSchematica {
    private static final Map<String, Data> registries = new ConcurrentHashMap<>();
    
    public interface Data {
        byte[] serialize();
        Data deserialize(byte[] data);
    }
    
    public static void register(Data data) {
        registries.put(data.getClass().getSimpleName().toLowerCase(),data);
    }
    
    public static Data deserialize(ByteBuffer buffer) {
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        if (buffer.remaining() < 4)
            throw new IllegalStateException("Invalid buffer: missing name length.");
        
        int length = buffer.getInt();
        if (length <= 0 || length > 256)
            throw new IllegalStateException("Invalid class name length: " + length);
        
        if (buffer.remaining() < length)
            throw new IllegalStateException("Buffer underflow: expected " + length + " bytes for class name.");
        
        byte[] name = new byte[length];
        buffer.get(name);
        
        String datatype = new String(name, StandardCharsets.UTF_8).toLowerCase();
        Data data = registries.get(datatype);
        if (data == null)
            throw new UnknownRegistryException("Registry for type '" + datatype + "' not found.");
        
        if (!buffer.hasRemaining())
            throw new IllegalStateException("No payload found for type '" + datatype + "'");
        
        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        
        return data.deserialize(payload);
    }
}
