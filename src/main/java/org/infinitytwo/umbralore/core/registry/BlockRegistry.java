package org.infinitytwo.umbralore.core.registry;

import org.infinitytwo.umbralore.block.BlockType;
import org.infinitytwo.umbralore.core.exception.IllegalDataTypeException;
import org.infinitytwo.umbralore.core.exception.UnknownRegistryException;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockRegistry {
    private final Map<Integer, BlockType> idToBlock = new ConcurrentHashMap<>();
    private final Map<String, Integer> nameToId = new ConcurrentHashMap<>();
    private final Map<Integer, DataSchematic> schematics = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Primitive> typeMap = new HashMap<>();

    static {
        typeMap.put(int.class, Primitive.INT);
        typeMap.put(Integer.class, Primitive.INT);
        typeMap.put(short.class, Primitive.SHORT);
        typeMap.put(Short.class, Primitive.SHORT);
        typeMap.put(long.class, Primitive.LONG);
        typeMap.put(Long.class, Primitive.LONG);
        typeMap.put(byte.class, Primitive.BYTE);
        typeMap.put(Byte.class, Primitive.BYTE);
        typeMap.put(double.class, Primitive.DOUBLE);
        typeMap.put(Double.class, Primitive.DOUBLE);
        typeMap.put(float.class, Primitive.FLOAT);
        typeMap.put(Float.class, Primitive.FLOAT);
    }

    private short nextId = 1;

    public int register(BlockType block) {
        int id = nextId++;
        idToBlock.put(id, block);
        nameToId.put(block.getId(), id);
        return id;
    }

    public int registerDynamicBlock(BlockType block) throws IllegalDataTypeException {
        Class<? extends BlockType> clazz = block.getClass();

        Field[] fields = clazz.getFields();
        DataSchematic schematic = new DataSchematic();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(Property.class)) continue;
            Class<?> datatype = field.getType();
            if (datatype.isInterface())
                throw new IllegalDataTypeException("Unsupported Class, the class \"" + datatype.getSimpleName() + "\" is a interface. Not Supported (for now).");
            Primitive primitive = typeMap.get(datatype);

            boolean serializable = false;
            if (primitive == null) {
                serializable = isSerializable(datatype);
            }
            if (primitive == null && !serializable)
                throw new IllegalDataTypeException("Unsupported Datatype, found \"" + datatype.getSimpleName() + "\"");

            schematic.add(field.getName(), primitive);
        }

        int id = register(block);
        schematics.put(id, schematic);

        return id;
    }

    public static boolean isSerializable(Class<?> datatype) {
        boolean serializable = false;
        for (Class<?> i : datatype.getInterfaces()) {
            if (i == Serializable.class) {
                serializable = true;
                break;
            }
        }
        return serializable;
    }

    public DataSchematic getDataSchematicOf(int id) {
        return schematics.getOrDefault(id, null);
    }

    public BlockType get(int id) {
        if (idToBlock.get(id) == null) throw new UnknownRegistryException("Couldn't find a registry with id: " + id);
        return idToBlock.get(id);
    }

    public int getId(String name) {
        return nameToId.get(name);
    }

    public Set<Integer> getIds() {
        return idToBlock.keySet();
    }

    public static class DataSchematic {
        private final Map<String, Primitive> dataOrder = new LinkedHashMap<>();

        public Set<String> getIds() {
            return dataOrder.keySet();
        }

        public void add(String id, Primitive primitive) {
            dataOrder.put(id, primitive);
        }

        public Primitive get(String id) {
            return dataOrder.getOrDefault(id, Primitive.NULL);
        }
    }

    public enum Primitive {
        INT,
        LONG,
        SHORT,
        BYTE,
        DOUBLE,
        FLOAT,
        NULL,
        OTHER;

        public static Class<?> toClass(Primitive primitive) {
            switch (primitive) {
                case DOUBLE -> {
                    return Double.class;
                }
                case SHORT -> {
                    return Short.class;
                }
                case FLOAT -> {
                    return Float.class;
                }
                case LONG -> {
                    return Long.class;
                }
                case BYTE -> {
                    return Byte.class;
                }
                case INT -> {
                    return Integer.class;
                }
                default -> {
                    return null;
                }
            }
        }

        public static Class<?> toPrimitiveClass(Primitive primitive) {
            switch (primitive) {
                case DOUBLE -> {
                    return double.class;
                }
                case SHORT -> {
                    return short.class;
                }
                case FLOAT -> {
                    return float.class;
                }
                case LONG -> {
                    return long.class;
                }
                case BYTE -> {
                    return byte.class;
                }
                case INT -> {
                    return int.class;
                }
                default -> {
                    return null;
                }
            }
        }
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Property {
    }

    public interface Serializable {
        int size();

        byte[] serialize();

        Object read(byte[] data);
    }
}
