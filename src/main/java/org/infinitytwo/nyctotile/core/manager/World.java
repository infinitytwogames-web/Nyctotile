package org.infinitytwo.nyctotile.core.manager;

import org.infinitytwo.nyctotile.core.Game;
import org.infinitytwo.nyctotile.core.ServerThread;
import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.ChunkPos;
import org.infinitytwo.nyctotile.core.data.SpawnLocation;
import org.infinitytwo.nyctotile.core.entity.Player;
import org.infinitytwo.nyctotile.core.intervals.Interval;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.network.ClientNetwork;
import org.infinitytwo.nyctotile.core.network.data.Packets;
import org.infinitytwo.nyctotile.core.registry.DimensionRegistry;
import org.infinitytwo.nyctotile.core.renderer.Camera;
import org.infinitytwo.nyctotile.core.world.GMap;
import org.infinitytwo.nyctotile.core.world.GridMap;
import org.infinitytwo.nyctotile.core.world.dimension.Dimension;

import java.util.*;

public class World {
    private Dimension current;
    private GridMap map;
    private ClientNetwork thread;
    private boolean connectionReq;
    private ServerThread serverThread;
    
    private final Set<ChunkPos> requested = Collections.synchronizedSet(new HashSet<>());
    private boolean dimensionRequest;
    private static final Map<String, Dimension> loadedDimension = new HashMap<>();
    private static long seed;
    private static SpawnLocation location;
    private final Interval clear = new Interval(5000, requested::clear);
    
    private static final World world = new World();
    private TextureAtlas atlas;
    private Camera camera;
    private Window window;
    private Player player;
    
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
    
    public static void setSpawnLocation(SpawnLocation location) {
        World.location = location;
    }
    
    public void draw(int view) {
        clear.update();
        if (map != null) {
            map.draw(camera, window, view);
            player.updateCamera((float) Game.getDelta());
        }
        if (thread == null) return;
        
        // --- Connection Initiation and BLOCKING Wait Logic ---
        if (connectionReq) {
            // 1. Start Client Thread
            if (serverThread != null && serverThread.getNetwork() != null) {
                serverThread.getNetwork().offlineMode(true);
                if (!serverThread.getNetwork().isStarted()) return;
            } else return;
            thread.start(); // This is non-blocking
            
            // 2. BLOCK THE MAIN THREAD until the handshake is complete.
            // This happens only once because 'connectionReq' is set to false afterward.
            try {
                // FIX 3: Keep the intended blocking call.
                thread.awaitHandshakeCompletion();
            } catch (InterruptedException e) {
                // Handle thread interruption gracefully
                Thread.currentThread().interrupt();
                throw new RuntimeException("Connection handshake interrupted", e);
            }
            
            connectionReq = false; // Connection process is finished (blocking part complete)
        }
        
        // --- Network State and World Loading Logic ---
        // If connectionReq is false, we skip the blocking section.
        // We now rely on the connection being ready.
        if (thread.isConnected()) {
            
            // GET THE CURRENT DIMENSION
            if (current == null && !dimensionRequest) {
                dimensionRequest = true;
                thread.send(new Packets.PCommand("getDimension"),true);
            }
            
            if (current == null) return;
            
            List<ChunkPos> missingChunks = map.getMissingSurroundingChunks(
                    GMap.worldToChunkPos((int) camera.getPosition().x, (int) camera.getPosition().z),
                    view
            );
            
            for (ChunkPos chunk : missingChunks) {
                if (requested.contains(chunk)) continue;
                
                requested.add(chunk);
                
                thread.send(new Packets.PCommand("getchunk " + chunk.x() + " " + chunk.z() + " " + DimensionRegistry.getRegistry().getId(current.getId())), true);
            }
        }
    }
    
    public Set<ChunkPos> getRequested() {
        return requested;
    }
    
    public void prepareForConnection(Camera camera, Window window, ClientNetwork networkThread, ServerThread thread, TextureAtlas atlas) {
        // 1. Assign all critical dependencies
        this.thread = networkThread;
        this.serverThread = thread;
        this.atlas = atlas;
        this.camera = camera;
        this.window = window;
        
        if (window == null || atlas == null || networkThread == null) {
            // Depending on your application's error handling, you might log an error,
            // throw an exception, or shut down gracefully here.
            System.err.println("FATAL: World dependencies not set correctly in prepareForConnection.");
            // Example: throw new IllegalStateException("Missing required initialization parameters.");
            return;
        }
        
        if (player == null) player = new Player(current,window,camera);
        clear.start();
    }
    
    public void connectToServer() {
        connectionReq = true;
    }
    
    public TextureAtlas getTextureAtlas() {
        return atlas;
    }
    
    public Player getLocalPlayer() {
        return player;
    }
}