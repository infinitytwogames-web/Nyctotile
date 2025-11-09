package org.infinitytwo.umbralore.core.manager;

import org.infinitytwo.umbralore.core.ServerThread;
import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.ChunkData;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.data.SpawnLocation;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.network.client.ClientNetworkThread;
import org.infinitytwo.umbralore.core.network.modern.ClientNetwork;
import org.infinitytwo.umbralore.core.network.modern.Packets;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.registry.DimensionRegistry;
import org.infinitytwo.umbralore.core.renderer.Camera;
import org.infinitytwo.umbralore.core.renderer.Chunk;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;

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
    private ClientNetwork thread;
    private boolean connectionReq;
    private ServerThread serverThread;
    private final List<ChunkPos> requested = new ArrayList<>();
    private boolean dimensionRequest;
    private final AtomicInteger requests = new AtomicInteger();
    
    private static final Map<String, Dimension> loadedDimension = new HashMap<>();
    private static long seed;
    private static SpawnLocation location;
    
    private static final World world = new World();
    private TextureAtlas atlas;
    
    public static Dimension getLoadedDimension(String dimension) {
        return loadedDimension.get(dimension);
    }
    
    public static void loadDimension(Dimension dimension) {
        loadedDimension.put(dimension.getId(), dimension);
    }
    
    public static Collection<Dimension> getLoadedDimensions() {
        return Collections.unmodifiableCollection(loadedDimension.values());
    }
    
    private World() {}
    
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
    
    public static World getInstance() {
        return world;
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
                serverThread.getNetwork().offlineMode(true);
                thread.start();
                connectionReq = false;
                try {
                    // Wait for the connection to establish AND the AES key exchange to finish
                    thread.awaitHandshakeCompletion();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            // GET THE CURRENT DIMENSION
            if (current == null && !dimensionRequest) {
                if (thread.isConnected()) {
                    dimensionRequest = true;
                    thread.send(new Packets.PCommand("getDimension"),true);
                }
            }
        }
        
        if (thread.isConnected()) {
            if (current == null) return;
            List<ChunkPos> chunks = map.getMissingSurroundingChunks((int) camera.getPosition().x, (int) camera.getPosition().z, 2);
            
            for (ChunkPos chunk : chunks) {
                if (requested.contains(chunk)) continue;
                requested.add(chunk);
                requests.incrementAndGet();
                thread.send(new Packets.PCommand("getchunk " + chunk.x() + " " + chunk.z() + " " + DimensionRegistry.getRegistry().getId(current.getId())), true);
            }
        }
    }
    
    private boolean isNetworkReady() {
        return current == null && serverThread != null && serverThread.getNetwork() != null &&
                serverThread.isAlive() && serverThread.getNetwork().isStarted() &&
                serverThread.isReady();
    }
    
    public void prepareForConnection(ClientNetwork networkThread, ServerThread thread, TextureAtlas atlas) {
        this.thread = networkThread;
        serverThread = thread;
        this.atlas = atlas;
    }
    
    public void connectToServer() {
        connectionReq = true;
    }
    
    public TextureAtlas getTextureAtlas() {
        return atlas;
    }
}