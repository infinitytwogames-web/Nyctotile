package org.infinitytwo.umbralore.core.data.io;

import org.infinitytwo.umbralore.block.BlockType;
import org.infinitytwo.umbralore.core.VectorMath;
import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.data.ItemType;
import org.infinitytwo.umbralore.core.entity.Entity;
import org.infinitytwo.umbralore.core.manager.EntityManager;
import org.infinitytwo.umbralore.core.manager.World;
import org.infinitytwo.umbralore.core.model.Model;
import org.infinitytwo.umbralore.core.registry.*;
import org.infinitytwo.umbralore.core.world.ServerProcedureGridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.joml.Vector2i;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.infinitytwo.umbralore.core.data.ChunkData.*;

public class WorldData {
    private static final ArrayList<String> dirsNeeded = new ArrayList<>();

    static {
        dirsNeeded.add("region");
        dirsNeeded.add("entity");
        dirsNeeded.add("registries"); // Added registry directory here
    }

    public static void save(String name) throws IOException {
        // ---- Creation ----
        Path path = Path.of("worlds", name);
        createDirs(path.toString());

        // ---- Saving Regions ----
        Collection<Dimension> dimensions = World.getLoadedDimensions();
        Map<Vector2i, Region> regions = new HashMap<>();

        for (Dimension dimension : dimensions) {
            ServerProcedureGridMap map = dimension.getWorld();

            Collection<ChunkData> chunks = map.getChunks();
            for (ChunkData chunk : chunks) {
                Vector2i position = convertToRegionPosition(chunk.getPosition());
                regions.computeIfAbsent(position, Region::new);

                regions.get(position).put(chunk);
            }

            for (Region region : regions.values()) {
                File file = Path.of("worlds",name,"region","region-"+ VectorMath.toStringAsId(region.position)+".ulr").toFile();
                // Use try-with-resources for reliable closing
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    stream.write(region.serialize());
                }
            }
        }

        // ---- Saving Entities ----
        Collection<Entity> entities = EntityManager.getAllEntities();
        File entitiesFile = Path.of("worlds",name,"entity","entities.ule").toFile();

        try (FileOutputStream stream = new FileOutputStream(entitiesFile)) {
            for (Entity entity : entities) stream.write(EntitySerializer.serialize(entity));
        }

        // ---- Saving Registries ----

        // This helper lambda writes an Integer followed by an Int length and the String bytes
        RegistrySerializer registrySerializer = (key, idString, filePath) -> {
            File file = Path.of("worlds",name,"registries",filePath).toFile();
            try (FileOutputStream stream = new FileOutputStream(file)) {

                byte[] idBytes = idString.getBytes(StandardCharsets.UTF_8);
                int length = idBytes.length;

                // Allocate buffer for the Integer Key + Integer Length
                ByteBuffer headerBuffer = ByteBuffer.allocate(Integer.BYTES * 2);

                headerBuffer.putInt(key);
                headerBuffer.putInt(length);

                stream.write(headerBuffer.array());
                stream.write(idBytes);
            }
        };

        // Block Registries
        Set<Map.Entry<Integer, BlockType>> blockEntries = BlockRegistry.getMainBlockRegistry().getEntries();
        for (Map.Entry<Integer, BlockType> entry : blockEntries) {
            registrySerializer.serialize(entry.getKey(), entry.getValue().getId(), "blocks.ulg");
        }

        // Entities
        Set<Map.Entry<Integer, Entity>> entityEntries = EntityRegistry.getRegistry().getEntries();
        for (Map.Entry<Integer, Entity> entry : entityEntries) {
            registrySerializer.serialize(entry.getKey(), entry.getValue().getId(), "entities.ulg");
        }

        // Model
        Set<Map.Entry<Integer, Model>> modelEntries = ModelRegistry.getEntries();
        for (Map.Entry<Integer, Model> entry : modelEntries) {
            registrySerializer.serialize(entry.getKey(), entry.getValue().getId(), "model.ulg");
        }

        // Item
        Set<Map.Entry<Integer, ItemType>> itemEntries = ItemRegistry.getMainRegistry().getEntries();
        for (Map.Entry<Integer, ItemType> entry : itemEntries) {
            registrySerializer.serialize(entry.getKey(), entry.getValue().getId(), "item.ulg");
        }

        // Dimensions
        Set<Map.Entry<Integer, Dimension>> dimensionEntries = DimensionRegistry.getRegistry().getEntries();
        for (Map.Entry<Integer, Dimension> entry : dimensionEntries) {
            registrySerializer.serialize(entry.getKey(), entry.getValue().getId(), "dimension.ulg");
        }

        // ---- Saving the actual world info ----
        File main = Path.of("worlds",name,"info.ulw").toFile();
        try (FileOutputStream stream = new FileOutputStream(main)) {
            stream.write(World.getSeed());
            stream.write("1.0.0".length());
            stream.write("1.0.0".getBytes(StandardCharsets.UTF_8));
            // TODO: MORE IMPORTANT INFO.
        }
    }

    public static void load(Path path) throws FileNotFoundException {
        // TODO: IMPLEMENT IMPORT
    }

    private static Vector2i convertToRegionPosition(Vector2i position) {
        // Converts global chunk position to the region grid position using floor division
        return new Vector2i(
                Math.floorDiv(position.x, Region.capacity),
                Math.floorDiv(position.y, Region.capacity)
        );
    }

    private static void createDirs(String root) throws IOException {
        for (String path : dirsNeeded) {
            Files.createDirectories(Path.of(root, path));
        }
    }

    public static void saveLevel(Dimension dimension, String name) {
        // TODO: IMPLEMENT
    }

    private static class Region {
        protected ChunkData[] chunks;
        // NOTE: SIZE must be defined elsewhere (e.g., 16*16 = 256 for capacity, or 16 for size X/Z)
        public static final int capacity = SIZE * SIZE; // Assumed 16x16=256 chunks per region
        protected Vector2i position;

        public Region(Vector2i position) {
            this.chunks = new ChunkData[capacity];
            this.position =  position;
        }

        protected byte[] serialize() {
            // 1. Filter out nulls and get the list of chunks actually saved
            List<ChunkData> actualChunks = Arrays.stream(chunks)
                    .filter(Objects::nonNull)
                    .toList();

            // If no chunks are loaded, only save the position info
            if (actualChunks.isEmpty()) {
                ByteBuffer emptyBuffer = ByteBuffer.allocate(2 * Integer.BYTES);
                emptyBuffer.putInt(position.x);
                emptyBuffer.putInt(position.y);
                return emptyBuffer.array();
            }

            // 2. Calculate the EXACT required size dynamically
            int totalChunkBytes = 0;
            // Pre-serialize all chunks to calculate total size and avoid re-serializing later
            List<byte[]> serializedChunks = new ArrayList<>(actualChunks.size());
            for (ChunkData chunk : actualChunks) {
                byte[] c = chunk.serialize();
                serializedChunks.add(c);
                totalChunkBytes += c.length;
            }

            // Total size =
            // 2 Integers for Region Position (X, Z)
            // + 1 Integer for the number of chunks (to help deserialization)
            // + total bytes from all serialized chunks
            int totalSize = (3 * Integer.BYTES) + totalChunkBytes;

            ByteBuffer buffer = ByteBuffer.allocate(totalSize);

            // 3. Put Region metadata
            buffer.putInt(position.x);
            buffer.putInt(position.y);
            buffer.putInt(actualChunks.size()); // New: Put chunk count for easy deserialization

            // 4. Put Chunk data
            for (byte[] c : serializedChunks) {
                buffer.put(c);
            }

            return buffer.array();
        }

        public void put(ChunkData chunk) {
            chunks[getIndex(conventToRegion(chunk.getPosition()))] = chunk;
        }

        private Vector2i conventToRegion(Vector2i position) {
            // Converts global chunk position to local position (0-15) within the region.
            int localX = Math.floorMod(position.x, SIZE);
            int localZ = Math.floorMod(position.y, SIZE);
            return new Vector2i(localX, localZ);
        }

        private int getIndex(Vector2i position) {
            return getIndex(position.x, position.y);
        }

        private int getIndex(int x, int y) {
            // Assumes SIZE is the width (16) of the region grid
            return (y * SIZE) + x;
        }
    }

    // Helper interface for cleaning up the repetitive registry serialization
    @FunctionalInterface
    private interface RegistrySerializer {
        void serialize(int key, String idString, String filePath) throws IOException;
    }
}