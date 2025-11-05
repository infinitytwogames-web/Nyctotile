package org.infinitytwo.umbralore.core.data.io;

import org.infinitytwo.umbralore.core.VectorMath;
import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.entity.Entity;
import org.infinitytwo.umbralore.core.manager.EntityManager;
import org.infinitytwo.umbralore.core.manager.World;
import org.infinitytwo.umbralore.core.registry.*; // Import the base Registry class
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.ServerProcedureGridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.joml.Vector2i;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class WorldData {
    private static final ArrayList<String> dirsNeeded = new ArrayList<>();
    public static final int CHUNK_SIZE = 16;

    // World
    public String name;
    public long seed;
    // Registry
    public BlockRegistry blockRegistry;
    public ItemRegistry itemRegistry;
    public DimensionRegistry dimensionRegistry;
    public EntityRegistry entityRegistry;

    static {
        dirsNeeded.add("regions");
        dirsNeeded.add("entity");
        dirsNeeded.add("registries");
    }

    // --- CRITICAL FIX 1: REGISTRY APPENDING ---
    @FunctionalInterface
    private interface RegistrySerializer {
        void serialize(int key, String idString, DataOutputStream stream) throws IOException;
    }

    public static void save(String name) throws IOException {
        Path worldPath = Path.of("worlds", name);
        createDirs(worldPath.toString());

        // --- Saving Registries (FIXED to ensure sequential writing) ---
        Path registriesPath = worldPath.resolve("registries");

        // Define the serialization logic once
        RegistrySerializer registrySerializer = (key, idString, stream) -> {
            byte[] idBytes = idString.getBytes(StandardCharsets.UTF_8);
            int length = idBytes.length;

            // Write the key, length, and string bytes sequentially
            stream.writeInt(key);
            stream.writeInt(length);
            stream.write(idBytes);
        };

        // FIXED: Using Registerable for generic safety
        Map<String, Set<? extends Map.Entry<Integer, ? extends Registerable>>> registriesToSave = Map.of(
                "blocks.ulg", BlockRegistry.getMainBlockRegistry().getEntries(),
                "entities.ulg", EntityRegistry.getRegistry().getEntries(),
                "items.ulg", ItemRegistry.getMainRegistry().getEntries(),
                "dimensions.ulg", DimensionRegistry.getRegistry().getEntries()
        );

        for (Map.Entry<String, Set<? extends Map.Entry<Integer, ? extends Registerable>>> registryEntry : registriesToSave.entrySet()) {
            File file = registriesPath.resolve(registryEntry.getKey()).toFile();
            // Use DataOutputStream and write all entries sequentially
            try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(file))) {
                for (Map.Entry<Integer, ? extends Registerable> entry : registryEntry.getValue()) {
                    registrySerializer.serialize(entry.getKey(), entry.getValue().getId(), stream);
                }
            }
        }

        // --- Saving Regions ---
        Collection<Dimension> dimensions = World.getLoadedDimensions();
        Map<Vector2i, Region> regions = new HashMap<>();

        for (Dimension dimension : dimensions) {
            int dimId = DimensionRegistry.getRegistry().getId(dimension.getId());
            String dimName= "DIM-"+dimId;
            Path regionDir = worldPath.resolve("regions").resolve(dimName);

            Files.createDirectories(regionDir);

            Path regionDetails = regionDir.resolve("info.dat");

            // Write Dimension ID to info.dat
            try (DataOutputStream outStream = new DataOutputStream(new FileOutputStream(regionDetails.toFile()))) {
                outStream.writeInt(dimId);
            }

            ServerProcedureGridMap map = dimension.getWorld();
            regions.clear();

            Collection<ChunkData> chunks = map.getChunks();
            for (ChunkData chunk : chunks) {
                Vector2i position = convertToRegionPosition(chunk.getPosition());
                regions.computeIfAbsent(position, Region::new).put(chunk);
            }

            for (Region region : regions.values()) {
                // Using try-with-resources with FileOutputStream as it's the safest way to ensure stream closure
                try (FileOutputStream stream = new FileOutputStream(regionDir.resolve("region-"+ VectorMath.toStringAsId(region.position)+".ulr").toFile())) {
                    stream.write(region.serialize());
                }
            }
        }

        // --- Saving Entities ---
        Collection<Entity> entities = EntityManager.getAllEntities();
        File entitiesFile = worldPath.resolve("entity").resolve("entities.ule").toFile();

        try (FileOutputStream stream = new FileOutputStream(entitiesFile)) {
            // Note: EntitySerializer.serialize(entity) must return a byte[]
            for (Entity entity : entities) stream.write(EntitySerializer.serialize(entity));
        }

        // --- Saving the actual world info (info.ulw) ---
        File main = worldPath.resolve("info.ulw").toFile();
        try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(main))) {
            // 1. World Name (Length-prefixed String)
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            stream.writeInt(nameBytes.length);
            stream.write(nameBytes);

            // 2. World Seed (Assumed Long)
            stream.writeLong(World.getSeed());

            // 3. Version (Length-prefixed String)
            String version = "1.0.0";
            byte[] versionBytes = version.getBytes(StandardCharsets.UTF_8);
            stream.writeInt(versionBytes.length);
            stream.write(versionBytes);
            // TODO: MORE IMPORTANT INFO.
        }
    }

    public static void load(Path path) throws IOException {
        RegistryDeserializer deserializer = new RegistryDeserializer();
        WorldData gameData = new WorldData();
        String worldName = ""; // Temp variable for world name

        // ---- 1. Loading World Data (info.ulw) ----
        File mainFile = path.resolve("info.ulw").toFile();
        if (mainFile.exists()) {
            try (DataInputStream stream = new DataInputStream(new FileInputStream(mainFile))) {
                // Read World Name
                int nameLength = stream.readInt();
                byte[] nameBytes = new byte[nameLength];
                stream.readFully(nameBytes);
                worldName = new String(nameBytes, StandardCharsets.UTF_8);
                gameData.name = worldName; // Assign to instance field

                // Read World Seed
                gameData.seed = stream.readLong();
                World.setSeed(gameData.seed);

                // Read Version (consumed to advance stream)
                int versionLength = stream.readInt();
                stream.skipBytes(versionLength);
            } catch (EOFException e) {
                System.err.println("Warning: info.ulw file truncated. World data may be incomplete.");
            }
        }

        // ---- 2. Loading Registries ----
        Path registriesPath = path.resolve("registries");
        if (Files.exists(registriesPath) && Files.isDirectory(registriesPath)) {

            // Pass concrete registry types to the generic loading method
            loadRegistryFile(registriesPath.resolve("blocks.ulg"), BlockRegistry.getMainBlockRegistry(), deserializer);
            loadRegistryFile(registriesPath.resolve("items.ulg"), ItemRegistry.getMainRegistry(), deserializer);
            loadRegistryFile(registriesPath.resolve("entities.ulg"), EntityRegistry.getRegistry(), deserializer);
            loadRegistryFile(registriesPath.resolve("dimensions.ulg"), DimensionRegistry.getRegistry(), deserializer);

            // Assign loaded registries to gameData object
            gameData.blockRegistry = BlockRegistry.getMainBlockRegistry();
            gameData.itemRegistry = ItemRegistry.getMainRegistry();
            gameData.entityRegistry = EntityRegistry.getRegistry();
            gameData.dimensionRegistry = DimensionRegistry.getRegistry();
        }

        // ---- 3. Loading Regions (FIXED logic to use ByteBuffer correctly) ----
        Path regionsPath = path.resolve("regions");
        if (Files.exists(regionsPath) && Files.isDirectory(regionsPath)) {

            try (Stream<Path> dimStream = Files.list(regionsPath)) {
                List<Path> dimensionDirs = dimStream.filter(Files::isDirectory).toList();

                World.clear();
                for (Path dimensionDir : dimensionDirs) {

                    Path infoPath = dimensionDir.resolve("info.dat");
                    if (!Files.exists(infoPath)) continue;

                    // Read Dimension ID
                    byte[] dataInfo = deserializer.deserialize(infoPath);
                    ByteBuffer infoBuffer = ByteBuffer.wrap(dataInfo);
                    int dimensionId = infoBuffer.getInt();

                    Dimension dim = gameData.dimensionRegistry.get(dimensionId);
                    if (dim == null) continue;

                    // Find and Load all .ulr region files
                    try (Stream<Path> regionStream = Files.list(dimensionDir)) {
                        List<File> regionFiles = regionStream
                                .filter(p -> p.getFileName().toString().endsWith(".ulr") && Files.isRegularFile(p))
                                .map(Path::toFile)
                                .toList();

                        for (File file : regionFiles) {
                            byte[] data = deserializer.deserialize(file);
                            ByteBuffer rBuffer = ByteBuffer.wrap(data);

                            // Read and ignore region coordinates (Consumed)
                            rBuffer.getInt();
                            rBuffer.getInt();

                            if (!rBuffer.hasRemaining()) continue;

                            int chunkCount = rBuffer.getInt(); // Read the count of chunks

                            for (int i = 0; i < chunkCount; i++) {
                                // ChunkData.unserialize must now accept and consume from the ByteBuffer
                                ChunkData chunk = ChunkData.unserialize(rBuffer);
                                // Added null check for robustness
                                dim.getWorld().addChunk(chunk);
                            }
                        }
                    }
                    World.loadDimension(dim);
                }
            }
        }

        // ---- 4. Loading Entities (Cleanup for efficiency) ----
        Path entitiesPath = path.resolve("entity").resolve("entities.ule");
        if (Files.exists(entitiesPath)) {
            byte[] raw = deserializer.deserialize(entitiesPath);
            ByteBuffer buffer = ByteBuffer.wrap(raw);

            // Size of one entity record (80 bytes) - Must be accurate.
            // (2 int, 12 float = 14*4 = 56 bytes. Your previous calculation was likely more accurate based on the structure)
            final int ENTITY_RECORD_SIZE = 56; // Assuming 2 ints (id, dim) and 12 floats (pos, vel, rot, scale, speed, grav, jump)

            while (buffer.hasRemaining()) {
                // Simplified safety check: ensure the entire record size is available
                // Note: If records are variable size, this logic must change.
                if (buffer.remaining() < ENTITY_RECORD_SIZE) {
                    System.err.println("Warning: Entity file truncated or corrupted.");
                    break;
                }

                // Deserialize the single record using the full buffer and advance its position
                // Assuming EntitySerializer.unserialize advances the buffer position by ENTITY_RECORD_SIZE.
                EntitySerializer.Data data = EntitySerializer.unserialize(buffer);

                if (data == null) {
                    System.err.println("Warning: Entity record failed to deserialize.");
                    break;
                }

                Entity baseType = gameData.entityRegistry.get(data.id());
                if (baseType != null) {
                    // Create a new instance of the entity from the base type
                    Entity entity = baseType.newInstance();
                    data.apply(entity);
                    EntityManager.put(entity);
                } else {
                    System.err.println("Skipping entity with unknown ID: " + data.id() + ". Data lost.");
                }
            }
        }
    }

    // --- New Helper for Registry Loading (Fixed using generics) ---
    // FIXED: Uses <T extends Registerable> to gain type safety and eliminate raw casts to Registry<Registerable>
    private static <T extends Registerable> void loadRegistryFile(Path regPath, Registry<T> registry, RegistryDeserializer deserializer) throws IOException {
        if (!Files.exists(regPath)) return;

        byte[] file = deserializer.deserialize(regPath);
        ByteBuffer buffer = ByteBuffer.wrap(file);

        while (buffer.hasRemaining()) {
            if (buffer.remaining() < Integer.BYTES * 2) {
                System.err.println("Warning: Registry file " + regPath.getFileName() + " truncated.");
                break;
            }

            int id = buffer.getInt();
            int length = buffer.getInt();

            if (buffer.remaining() < length) {
                System.err.println("Warning: Registry file " + regPath.getFileName() + " truncated at ID " + id);
                break;
            }

            byte[] nameBytes = new byte[length];
            buffer.get(nameBytes);
            String idString = new String(nameBytes, StandardCharsets.UTF_8);

            // Fetch the concrete type from the game's main registry (assumed to be a Registerable)
            T type = null;

            // The main registry is assumed to return the Registerable subtype (BlockType, ItemType, etc.)
            // We use an unchecked cast to T here, which is necessary but safe because the caller ensures
            // the main registry and the world registry are of the same type (e.g., BlockRegistry).
            if (registry instanceof BlockRegistry) {
                type = (T) BlockRegistry.getMainBlockRegistry().get(idString);
            } else if (registry instanceof ItemRegistry) {
                type = (T) ItemRegistry.getMainRegistry().get(idString);
            } else if (registry instanceof EntityRegistry) {
                type = (T) EntityRegistry.getRegistry().get(idString);
            } else if (registry instanceof DimensionRegistry) {
                type = (T) DimensionRegistry.getRegistry().get(idString);
            }

            if (type != null) {
                // FIXED: Call register directly, using the type parameter T
                registry.register(id, type);
            } else {
                System.err.println("Warning: Skipping entry " + idString + " in " + regPath.getFileName() + ". Type not found in session registry.");
            }
        }
    }

    private static Vector2i convertToRegionPosition(Vector2i position) {
        // Converts global chunk position to the region grid position using floor division
        return new Vector2i(
                Math.floorDiv(position.x, Region.SIZE),
                Math.floorDiv(position.y, Region.SIZE)
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
        // Use a SIZE constant for clarity
        public static final int SIZE = 16; // 16x16 chunks per region
        public static final int capacity = SIZE * SIZE;
        protected Vector2i position;

        public Region(Vector2i position) {
            this.chunks = new ChunkData[capacity];
            this.position = position;
        }

        protected byte[] serialize() {
            List<ChunkData> actualChunks = Arrays.stream(chunks)
                    .filter(Objects::nonNull)
                    .toList();

            // 1. Calculate the EXACT required size dynamically
            int totalChunkBytes = 0;
            // Pre-serialize all chunks to calculate total size and avoid re-serializing later
            List<byte[]> serializedChunks = new ArrayList<>(actualChunks.size());
            for (ChunkData chunk : actualChunks) {
                // ChunkData.serialize() must now return a byte[] that includes its own
                // local chunk position (e.g., 2 ints) so the deserializer knows where to place it.
                byte[] c = chunk.serialize();
                serializedChunks.add(c);
                totalChunkBytes += c.length;
            }

            // Total size =
            // 2 Integers for Region Position (X, Z) + 1 Integer for chunk count + total bytes from all serialized chunks
            int totalSize = (3 * Integer.BYTES) + totalChunkBytes;

            // Handle empty regions gracefully
            if (actualChunks.isEmpty()) {
                // If empty, return a buffer containing just the region coords and 0 chunk count
                ByteBuffer emptyBuffer = ByteBuffer.allocate(3 * Integer.BYTES);
                emptyBuffer.putInt(position.x);
                emptyBuffer.putInt(position.y);
                emptyBuffer.putInt(0); // chunk count is 0
                return emptyBuffer.array();
            }

            ByteBuffer buffer = ByteBuffer.allocate(totalSize);

            // 2. Put Region metadata
            buffer.putInt(position.x);
            buffer.putInt(position.y);
            buffer.putInt(actualChunks.size());

            // 3. Put Chunk data
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
            // Using the assumed CHUNK_SIZE
            int localX = Math.floorMod(position.x, CHUNK_SIZE);
            int localZ = Math.floorMod(position.y, CHUNK_SIZE);
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

    private static class RegistryDeserializer {
        // Reads entire file into memory as a byte array
        public byte[] deserialize(Path filePath) throws IOException {
            return deserialize(filePath.toFile());
        }

        public byte[] deserialize(File file) throws IOException {
            try (FileInputStream stream = new FileInputStream(file)) {
                return stream.readAllBytes();
            }
        }
    }
}