package org.infinitytwo.umbralore.core.data.io;

import org.infinitytwo.umbralore.core.exception.UnknownRegistryException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSchematica {
    private static final Map<String, Data> registries = new ConcurrentHashMap<>();
    
    public interface Data {
        byte[] serialize();
        Data unserialize(byte[] data);
    }
    
    public static void register(Data data) {
        registries.put(data.getClass().getSimpleName().toLowerCase(),data);
    }
    
    public static Data unserialize(ByteBuffer buffer) {
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int length = buffer.getInt();
        byte[] name = new byte[length];
        buffer.get(name); // FAILED HERE
        String datatype = new String(name, StandardCharsets.UTF_8).toLowerCase();
        Data data = registries.getOrDefault(datatype,null);
        if (data == null) throw new UnknownRegistryException("The registry: "+datatype+" does not exists.");
        byte[] finalData = new byte[buffer.remaining()];
        buffer.get(finalData);
        return data.unserialize(finalData);
    }
}
