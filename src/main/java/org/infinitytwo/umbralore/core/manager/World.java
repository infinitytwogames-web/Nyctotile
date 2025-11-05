package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.ServerThread;
import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.data.SpawnLocation;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.network.client.ClientNetworkThread;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.registry.DimensionRegistry;
import org.infinitytwo.umbralore.core.renderer.Camera;
import org.infinitytwo.umbralore.core.renderer.Chunk;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.joml.Vector2i;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinitytwo.umbralore.core.constants.PacketType.CMD_BYTE_DATA;
import static org.infinitytwo.umbralore.core.constants.PacketType.COMMAND;

public class World {
    private Dimension current;
    private GridMap map;
    private ClientNetworkThread thread;
    private boolean connectionReq;
    private ServerThread serverThread;
    private final List<ChunkPos> requested = new ArrayList<>();
    private boolean dimensionRequest;
    private final AtomicInteger requests = new AtomicInteger();
    private final int max = 1;
    
    private static final Map<String, Dimension> loadedDimension = new HashMap<>();
    private static long seed;
    private static SpawnLocation location;
    
    public static Dimension getLoadedDimension(String dimension) {
        return loadedDimension.get(dimension);
    }
    
    public static void loadDimension(Dimension dimension) {
        loadedDimension.put(dimension.getId(), dimension);
    }
    
    public static Collection<Dimension> getLoadedDimensions() {
        return Collections.unmodifiableCollection(loadedDimension.values());
    }
    
    public static long getSeed() {
        return seed;
    }
    
    public static void setSeed(long seed) {
        World.seed = seed;
    }
    
    public static void clear() {
        loadedDimension.clear();
        seed = 0;
    }
    
    public static SpawnLocation getSpawnLocation() {
        return location;
    }
    
    public Dimension getCurrent() {
        return current;
    }
    
    public void setCurrent(Dimension current) {
        this.current = current;
    }
    
    public GridMap getMap() {
        return map;
    }
    
    public void setMap(GridMap map) {
        this.map = map;
    }
    
    public static SpawnLocation getLocation() {
        return location;
    }
    
    public static void setLocation(SpawnLocation location) {
        World.location = location;
    }
    
    public void draw(Camera camera, Window window, int view) {
        if (map != null) map.draw(camera, window, view);
        if (thread == null) return;
        
        if (isNetworkReady()) {
            // CONNECT TO SERVER
            if (connectionReq) {
                try {
                    serverThread.getNetworkThread().offlineMode(true);
                    thread.connect(InetAddress.getByName("127.0.0.1"), serverThread.getNetworkThread().getPort());
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
                connectionReq = false;
            }
            
            // GET THE CURRENT DIMENSION
            if (current == null && !dimensionRequest) {
                if (thread.isConnected()) {
                    dimensionRequest = true;
                    thread.send("getDimension",true,true);
                }
            }
        }
        
        if (thread.isConnected()) {
            if (current == null) return;
            List<ChunkPos> chunks = map.getMissingSurroundingChunks((int) camera.getPosition().x, (int) camera.getPosition().z, 2);
            
            for (ChunkPos chunk : chunks) {
                if (requested.contains(chunk) || requests.get() >= max) continue;
                requested.add(chunk);
                requests.incrementAndGet();
                thread.send("getchunk " + chunk.x() + " " + chunk.z() + " " + DimensionRegistry.getRegistry().getId(current.getId()), true, true);
            }
        }
    }
    
    private boolean isNetworkReady() {
        return current == null && serverThread != null &&
                serverThread.isAlive() &&
                thread.isReady() &&
                serverThread.isReady() && serverThread.getNetworkThread().isReady();
    }
    
    public void prepareForConnection(ClientNetworkThread networkThread, ServerThread thread, TextureAtlas atlas) {
        this.thread = networkThread;
        serverThread = thread;
        
        networkThread.getNetworkHandler().setProcessor((packet, t) -> {
            if (packet.payload() == null || packet.payload().length < Integer.BYTES) {
                return;
            }
            
            if (packet.type() == CMD_BYTE_DATA.getByte()) {
                ByteBuffer pack = ByteBuffer.wrap(packet.payload());
                
                int commandLength = pack.getInt();
                
                if (pack.remaining() < commandLength) {
                    System.err.println("Packet corruption or incomplete CMD_BYTE_DATA payload received.");
                    return;
                }
                
                // 2. Read Command String
                byte[] commandBytes = new byte[commandLength];
                pack.get(commandBytes);
                String fullCommand = new String(commandBytes, StandardCharsets.UTF_8);
                String[] args = fullCommand.split("\\s+");
                String command = args[0];
                
                // 3. Process Chunk Data
                if (command.equals("chunk") && args.length >= 3) {
                    try {
                        int chunkX = Integer.parseInt(args[1]);
                        int chunkZ = Integer.parseInt(args[2]); // Coordinates extracted from command string
                        
                        // 4. Extract the remaining raw data payload (the actual block data)
                        int rawDataLength = pack.remaining();
                        byte[] rawBlockData = new byte[rawDataLength];
                        pack.get(rawBlockData);
                        
                        ByteBuffer buffer = ByteBuffer.allocate(rawDataLength + (2 * Integer.BYTES));
                        
                        // Add the coordinates we read from the command string
                        buffer.putInt(chunkX);
                        buffer.putInt(chunkZ);
                        
                        // Add the raw block data from the packet payload
                        buffer.put(rawBlockData);
                        buffer.flip(); // Prepare for reading
                        
                        ChunkData data = ChunkData.unserialize(buffer);
                        
                        // The map.addChunk seems to use the ChunkData object which should contain the coords now
                        WorkerThreads.dispatch(() -> {
                            Chunk chunk = Chunk.of(data, map, atlas, BlockRegistry.getMainBlockRegistry());
                            map.addChunk(chunk);
                            System.out.println("CHUNK HAS BEEN CREATED: "+chunk.getPosition());
                            requests.decrementAndGet();
                            synchronized (requested) {
                                requested.remove(new ChunkPos(chunkX, chunkZ));
                            }
                        });
                        
                    } catch (NumberFormatException e) {
                        System.err.println("Client Error: Received 'chunk' command with invalid coordinates: " + fullCommand);
                    }
                }
            } else if (packet.type() == COMMAND.getByte()) {
                String[] args = new String(packet.payload(), StandardCharsets.UTF_8).split("\\s+");
                String command = args[0];
                
                if (command.equals("dimension")) {
                    System.out.println(args[1]);
                    current = DimensionRegistry.getRegistry().get(args[1]);
                    dimensionRequest = false;
                }
            }
        });
    }
    
    public void connectToServer() {
        connectionReq = true;
    }
}