package org.infinitytwo.nyctotile.core.data.io;

import org.infinitytwo.nyctotile.block.BlockType;
import org.infinitytwo.nyctotile.core.exception.IllegalDataTypeException;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Set;

public final class BlockDataReader {
    private final Logger logger = LoggerFactory.getLogger(BlockDataReader.class);
    private final BlockRegistry registry;

    public BlockDataReader(BlockRegistry registry) {
        this.registry = registry;
    }

    public <T extends BlockType> T fillData(T blockType, byte[] data, int id) {
        BlockRegistry.DataSchematic schematic = registry.getDataSchematicOf(id);
        if (schematic == null) throw new IllegalArgumentException(
                "The BlockType \"" + blockType.getName() + "\" is not a dynamic block."
        );

        ByteBuffer buffer = ByteBuffer.wrap(data);

        for (String fieldName : schematic.getIds()) {
            Object o = switch (schematic.get(fieldName)) {
                case INT -> buffer.getInt();
                case BYTE -> buffer.get();
                case LONG -> buffer.getLong();
                case FLOAT -> buffer.getFloat();
                case SHORT -> buffer.getShort();
                case DOUBLE -> buffer.getDouble();
                default -> null;
            };

            try {
                Field field = blockType.getClass().getField(fieldName);
                field.set(blockType, o);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return blockType;
    }

    public <T extends BlockType> byte[] serialize(T blockType, int id) {
        BlockRegistry.DataSchematic schematic = registry.getDataSchematicOf(id);

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(byteOutStream);

        for (String fieldName : schematic.getIds()) {
            try {
                Field field = blockType.getClass().getField(fieldName);
                field.setAccessible(true);

                switch (schematic.get(fieldName)) {
                    case SHORT -> outputStream.writeShort(field.getShort(blockType));
                    case DOUBLE -> outputStream.writeDouble(field.getDouble(blockType));
                    case FLOAT -> outputStream.writeFloat(field.getFloat(blockType));
                    case LONG -> outputStream.writeLong(field.getLong(blockType));
                    case BYTE -> outputStream.write(field.getByte(blockType));
                    case INT -> outputStream.writeInt(field.getInt(blockType));
                    case OTHER -> {
                        Class<?> type = field.getType();
                        if (BlockRegistry.isSerializable(type)) { // safety check
                            Method method = type.getMethod("serialize");
                            Object result = method.invoke(field.get(blockType));
                            if (result instanceof byte[] bytes) {
                                outputStream.write(bytes);
                            }
                        }
                    }
                    default -> throw new IllegalStateException("Schematic seems to be corrupted. Could not continue!");
                }

            } catch (NoSuchFieldException | IOException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        return byteOutStream.toByteArray();
    }

    public void setData(int id, byte[] dataset, String field, Object value) {
        if (field.isEmpty()) return;

        BlockRegistry.DataSchematic schematic = registry.getDataSchematicOf(id);
        Set<String> ids = schematic.getIds();
        int offset = 0;
        ByteBuffer buffer = ByteBuffer.wrap(dataset);

        for (String Id : ids) {
            try {
                switch (schematic.get(Id)) {
                    case INT -> {
                        if (Id.equals(field)) {
                            buffer.putInt(offset, (Integer) value);
                            break;
                        }
                        offset += Integer.BYTES;
                    }
                    case BYTE -> {
                        if (Id.equals(field)) {
                            buffer.put(offset, (Byte) value);
                            break;
                        }
                        offset += Byte.BYTES;
                    }
                    case LONG -> {
                        if (Id.equals(field)) {
                            buffer.putLong(offset, (Long) value);
                            break;
                        }
                        offset += Long.BYTES;
                    }
                    case FLOAT -> {
                        if (Id.equals(field)) {
                            buffer.putFloat(offset, (Float) value);
                            break;
                        }
                        offset += Float.BYTES;
                    }
                    case SHORT -> {
                        if (Id.equals(field)) {
                            buffer.putShort(offset, (Short) value);
                            break;
                        }
                        offset += Short.BYTES;
                    }
                    case DOUBLE -> {
                        if (Id.equals(field)) {
                            buffer.putDouble(offset, (Double) value);
                            break;
                        }
                        offset += Double.BYTES;
                    }
                    case OTHER -> {
                        if (BlockRegistry.isSerializable(value.getClass())) {
                            if (Id.equals(field)) {
                                Method method = value.getClass().getMethod("serialize");
                                byte[] bytes = (byte[]) method.invoke(value);
                                buffer.put(bytes);
                                break;
                            }
                        }
                        Method size = value.getClass().getMethod("size");
                        offset += (int) size.invoke(value);
                    }
                }
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("There is a mismatch datatypes between inputted data and the field \""+field+".\"");
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("There might be a mismatched datatypes between inputted data and the field \""+field+".\"",e);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Data getData(int id, byte[] dataset, String field) throws IllegalDataTypeException {
        if (field.isEmpty()) return null;
        if (dataset == null) return null;

        BlockRegistry.DataSchematic schematic = registry.getDataSchematicOf(id);
        Set<String> ids = schematic.getIds();
        int offset = 0;
        ByteBuffer buffer = ByteBuffer.wrap(dataset);

        for (String Id : ids) {
            switch (schematic.get(Id)) {
                case INT -> {
                    if (Id.equals(field)) return new Data(buffer.getInt(offset), BlockRegistry.Primitive.INT);
                    offset += Integer.BYTES;
                }
                case BYTE -> {
                    if (Id.equals(field)) return new Data(buffer.get(offset), BlockRegistry.Primitive.BYTE);
                    offset += Byte.BYTES;
                }
                case LONG -> {
                    if (Id.equals(field)) return new Data(buffer.getLong(offset), BlockRegistry.Primitive.LONG);
                    offset += Long.BYTES;
                }
                case FLOAT -> {
                    if (Id.equals(field)) return new Data(buffer.getFloat(offset), BlockRegistry.Primitive.FLOAT);
                    offset += Float.BYTES;
                }
                case SHORT -> {
                    if (Id.equals(field)) return new Data(buffer.getShort(offset), BlockRegistry.Primitive.SHORT);
                    offset += Short.BYTES;
                }
                case DOUBLE -> {
                    if (Id.equals(field)) return new Data(buffer.getDouble(offset), BlockRegistry.Primitive.DOUBLE);
                    offset += Double.BYTES;
                }
                case OTHER -> throw new IllegalArgumentException("Couldn't retrieve the value, because it's not a primitive.");
                default -> {
                    logger.warn("field \""+field+"\" has undefined primitive!");
                    throw new IllegalDataTypeException("field \""+field+"\" has undefined primitive!");
                }
            }
        }
        return new Data(null, BlockRegistry.Primitive.NULL);
    }

    public <T extends BlockRegistry.Serializable> Data getDataOfType(int id, byte[] dataset, String field, T type) throws IllegalDataTypeException {
        if (field.isEmpty()) return null;
        if (dataset == null) return null;

        BlockRegistry.DataSchematic schematic = registry.getDataSchematicOf(id);
        Set<String> ids = schematic.getIds();
        int offset = 0;
        ByteBuffer buffer = ByteBuffer.wrap(dataset);

        for (String Id : ids) {
            switch (schematic.get(Id)) {
                case INT -> offset += Integer.BYTES;
                case BYTE -> offset += Byte.BYTES;
                case LONG -> offset += Long.BYTES;
                case FLOAT -> offset += Float.BYTES;
                case SHORT -> offset += Short.BYTES;
                case DOUBLE -> offset += Double.BYTES;
                case OTHER -> {
                    if (Id.equals(field)) {
                        int size = type.size();
                        byte[] bytes = new byte[size];
                        buffer.get(offset, bytes);
                        return new Data(type.read(bytes), BlockRegistry.Primitive.OTHER);
                    }
                }
                default -> {
                    logger.warn("field \""+field+"\" has undefined primitive!");
                    throw new IllegalDataTypeException("field \""+field+"\" has undefined primitive!");
                }
            }
        }
        return new Data(null, BlockRegistry.Primitive.NULL);
    }

    public static class Data {
        public Object value;
        public BlockRegistry.Primitive type;

        public Data(Object o, BlockRegistry.Primitive type) {
            this.type = type;
            value = o;
        }
    }
}
