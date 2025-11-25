package org.infinitytwo.nyctotile.core;

import org.infinitytwo.nyctotile.block.ServerBlockType;
import org.infinitytwo.nyctotile.core.data.Block;
import org.infinitytwo.nyctotile.core.data.ChunkPos;
import org.infinitytwo.nyctotile.core.data.PlayerData;
import org.infinitytwo.nyctotile.core.data.SpawnLocation;
import org.infinitytwo.nyctotile.core.entity.Entity;
import org.infinitytwo.nyctotile.core.entity.Player;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.exception.UnknownRegistryException;
import org.infinitytwo.nyctotile.core.manager.EntityManager;
import org.infinitytwo.nyctotile.core.manager.Players;
import org.infinitytwo.nyctotile.core.manager.World;
import org.infinitytwo.nyctotile.core.network.data.Packets;
import org.infinitytwo.nyctotile.core.network.ServerNetwork;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.registry.DimensionRegistry;
import org.infinitytwo.nyctotile.core.security.AntiCheat;
import org.infinitytwo.nyctotile.core.world.ServerProcedureGridMap;
import org.infinitytwo.nyctotile.core.world.dimension.Dimension;
import org.infinitytwo.nyctotile.core.world.dimension.Overworld;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.UUID;

public class ServerThread extends Thread {
    private volatile boolean closing = false;
    private volatile boolean ready = false;
    private final EventBus eventBus = new EventBus("Server Network Thread");
    private Dimension overworld;
    private int seed;
    private BlockRegistry registry;
    private ServerBlockType grass;
    private ServerBlockType dirt;
    private ServerBlockType mantle;
    private ServerBlockType stone;
    private final Logger logger = LoggerFactory.getLogger(ServerThread.class);
    
    private final AntiCheat antiCheat = new AntiCheat(9);
    private ServerNetwork network;
    private final double fixedDelta = 1.0 / 60.0;
    private double delta;
    private double accumulator;
    
    public ServerThread(int seed) {
        eventBus.register(this);
        setName("Server Thread");
        
        this.seed = seed;
    }
    
    public ServerProcedureGridMap getCurrentWorld() {
        return overworld.getWorld();
    }
    
    @Override
    public void run() {
        logger.info("Constructing Server...");
        construct();
        logger.info("Initiating Server...");
        init();
        
        ready = true;
        logger.info("Server is ready");
        double lastTime = System.nanoTime();
        
        while (!closing) {
            // 1. Get the current time
            double currentTime = System.nanoTime();
            
            // 2. Calculate delta (Current - Last)
            // Convert to seconds: (currentTime - lastTime) / 1,000,000,000.0
            delta = (currentTime - lastTime) / 1_000_000_000.0;
            
            // 3. Update lastTime for the next iteration
            lastTime = currentTime;
            
            accumulator += delta; // accumulator now correctly accumulates positive time
            
            // --- fixed-step physics loop (run here, not in render) ---
            while (accumulator >= fixedDelta) {
                applyPhysics(); // <-- THIS WILL NOW RUN!
                accumulator -= fixedDelta;
            }
            
            tick();
        }
        logger.info("Closing!");
        cleanup();
    }
    
    private void applyPhysics() {
        Collection<Entity> entities = EntityManager.getAllEntities();
        
        for (Entity entity : entities) {
            // Server only runs physics for NON-Player entities (e.g., NPCs, mobs)
            if (!(entity instanceof Player)) {
                entity.update((float) fixedDelta);
            }
            
            if (!entity.getPosition().equals(entity.getPrevPosition())) {
                if (entity instanceof Player player) {
                    // If the entity is a player, the server MUST NOT broadcast its position
                    // based on server-side physics, as that physics is now DISABLED.
                    // The position will be broadcast when the client sends the "setposition" command.
                    
                    // We do nothing here for the player entity for now,
                    // as its position will be updated and broadcasted by the new command handler (Action B).
                } else {
                    // Keep the broadcast logic for all other entities (NPCs, Mobs, etc.)
                    network.broadcastUDP(
                            new Packets.PPosition(
                                    entity.getUUID().getLeastSignificantBits(),
                                    entity.getUUID().getMostSignificantBits(),
                                    entity.getPosition().x,
                                    entity.getPosition().y,
                                    entity.getPosition().z
                            )
                    );
                }
            }
        }
    }
    
    private void construct() {
        seed = (int) Math.floor(Math.random() * 1000000);
        registry = new BlockRegistry();
        overworld = new Overworld(seed, registry);
        
        grass = new ServerBlockType("soil", false, "grass_block");
        dirt = new ServerBlockType("soil", false, "dirt");
        stone = new ServerBlockType("stone", false, "stone");
        mantle = new ServerBlockType("mantle", false, "mantle");
        
        registry.register(grass);
        registry.register(dirt);
        registry.register(stone);
        registry.register(mantle);
        
        DimensionRegistry.getRegistry().register(overworld);
        
        network = new ServerNetwork(5896, 4789, ((packet, connection) -> {
            // --- HANDLER: PCommand (Simple commands, e.g., 'getdimension') ---
            if (packet instanceof Packets.PCommand pCommand) {
                String line = pCommand.command().toLowerCase().trim();
                String[] args = line.split("\\s+");
                if (args.length == 0) return;
                logger.info("Got command: {}", line);
                String cmd = args[0];
                
                if (cmd.equals("getdimension")) {
                    Player player = Players.getPlayer(Players.getPlayerByConnection(connection));
                    if (player != null) {
                        network.send(new Packets.PCommand("dimension " + player.getDimension().getId()), connection, true);
                    } else {
                        network.sendFailure(connection, "Player not authenticated.");
                    }
                } else if (cmd.equals("setvelocity")) {
                    if (args.length >= 4) {
                        float x, y, z;
                        try {
                            x = Float.parseFloat(args[1]);
                            y = Float.parseFloat(args[2]);
                            z = Float.parseFloat(args[3]);
                        } catch (Exception e) {
                            logger.error("ERROR: Float parsing failed for velocity command.", e);
                            
                            // FIX 2: Change message to reflect the correct input type
                            network.sendFailure(connection, "Non-standard input has been detected: Velocity components must be valid numbers (float).");
                            return;
                        }
                        
                        Player player = Players.getPlayer(Players.getPlayerByConnection(connection));
                        
                        if (player != null) {
//                            synchronized (player) {
//                                if (antiCheat.isAcceptableVelocity(x, y, z, player)) {
//                                    if (y > 0) {
//                                        player.setVelocity(x, y, z); // Trust client jump/up movement
//                                    } else {
//                                        Vector3f currentVelocity = player.getVelocity(); // Get synchronized copy
//
//                                        player.setVelocity(x, currentVelocity.y, z);
//                                    }
//                                }
//                            }
                            
                            logger.error("Service Unavaliable: set velocity is deprecated!");
                            network.send(new Packets.Failure("SERVICE_ERROR Service Unavaliable: set velocity is deprecated!"),connection,true);
                        }
                    }
                } else if (cmd.equals("setposition")) {
                    if (args.length >= 4) {
                        float x, y, z;
                        try {
                            x = Float.parseFloat(args[1]);
                            y = Float.parseFloat(args[2]);
                            z = Float.parseFloat(args[3]);
                        } catch (Exception e) {
                            network.sendFailure(connection, "Position components must be valid numbers (float).");
                            return;
                        }
                        
                        Player player = Players.getPlayer(Players.getPlayerByConnection(connection));
                        
                        if (player != null) {
                            // 1. Synchronize to ensure thread safety when modifying position
                            synchronized (player) {
                                // 2. Anti-cheat: Validate the reported position
                                if (antiCheat.isAcceptablePosition(x, y, z, player)) {
                                    // 3. Set the player's new authoritative position
                                    player.setPosition(x, y, z);
                                    
                                    // 4. Broadcast the new position to all other clients
                                    network.broadcastUDPExcept(
                                            connection,  // Exclude the sending client
                                            new Packets.PPosition(
                                                    player.getUUID().getLeastSignificantBits(),
                                                    player.getUUID().getMostSignificantBits(),
                                                    x, y, z
                                            )
                                    );
                                } else {
                                    logger.warn("Anti-cheat System has detected a violation at client {}", player.getData().name());
                                    network.send(new Packets.PPosition(0, 0, player.getPosition().x, player.getPosition().y, player.getPosition().z), connection, true);
                                }
                            }
                        }
                    }
                } else if (cmd.startsWith("getchunk")) {
                    if (args.length < 4) return;
                    int x, y, dimension;
                    
                    try {
                        x = Integer.parseInt(args[1]);
                        y = Integer.parseInt(args[2]);
                        dimension = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        logger.error("ERROR: Int parsing failed for getchunk command.", e);
                        network.sendFailure(connection, "Chunk coordinates or dimension ID must be integers.");
                        return;
                    }
                    
                    PlayerData playerData = Players.getPlayerByConnection(connection);
                    Player player = Players.getPlayer(playerData);
                    
                    if (player == null) {
                        logger.error("Couldn't access the PlayerData");
                        network.sendFailure(connection, "Player not authenticated or entity missing.");
                        return;
                    }
                    
                    if (antiCheat.shouldSendRequestedChunk(x, y, player.getPosition())) {
                        Dimension d;
                        try {
                            d = DimensionRegistry.getRegistry().get(dimension);
                        } catch (UnknownRegistryException e) {
                            logger.error("Failed to get the dimension id: {}", dimension);
                            network.sendFailure(connection, "Dimension id is invalid.");
                            return;
                        }
                        
                        // Retrieve or generate the chunk
                        try {
                            // Assuming getChunk returns an object that can be converted to int[] blocks
                            int[] blocks = d.getWorld().getChunkOrGenerate(new Vector2i(x, y)).getBlockIds();
                            
                            // FIX 1: Instantiate the PChunk object and send it directly.
                            // The Network.send method will handle the encryption and wrapping.
                            network.send(new Packets.PChunk(x, y, blocks), connection, true);
                            
                        } catch (Exception e) {
                            logger.error("Error retrieving/generating chunk ({}, {}) in dimension {}.", x, y, dimension, e);
                            network.sendFailure(connection, "Server failed to load requested chunk.");
                        }
                        
                    } else {
                        network.sendFailure(connection, "Anti-Cheat system has detected suspicious activity from this client");
                        logger.warn("Anti-Cheat system has detected a violation at client \"{}\"", playerData.name());
                    }
                } else if (cmd.equals("getposition")) {
                    if (args[1].equals("self")) {
                        PlayerData playerData = Players.getPlayerByConnection(connection);
                        
                        if (playerData != null) {
                            Vector3f player = Players.getPlayer(playerData).getPosition();
                            network.send(new Packets.PPosition(0, 0, player.x, player.y, player.z), connection, false);
                        }
                    } else {
                        Entity entity = EntityManager.getEntityFromId(UUID.fromString(args[1]));
                        if (entity == null) {
                            network.sendFailure(connection, "Entity does not exists.");
                        } else
                            network.send(new Packets.PPosition(entity.getUUID().getLeastSignificantBits(), entity.getUUID().getMostSignificantBits(), entity.getPosition().x, entity.getPosition().y, entity.getPosition().z), connection, false);
                    }
                }
            }
        }));
    }
    
    private void init() {
        overworld.generateSync(0, 0);
        Block block = overworld.getWorld().getTopBlock(0,0);
        System.out.println(block);
        if (block != null) World.setSpawnLocation(new SpawnLocation(VectorMath.toFloat(block.getPosition().add(0,1,0)),overworld));
        else throw new RuntimeException();
        
        
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                overworld.generate(new ChunkPos(x, y));
            }
        }
        
        network.start();
    }
    
    private void tick() {
        for (int id : registry.getIds()) {
            if (!(registry.get(id) instanceof ServerBlockType)) throw new RuntimeException("E");
        }
        try {
            Thread.sleep(10); // To prevent high CPU usage
        } catch (InterruptedException e) {
            cleanup();
        }
    }
    
    public ServerNetwork getNetwork() {
        return network;
    }
    
    private void cleanup() {
        network.shutdown();
    }
    
    public void shutdown() {
        closing = true;
    }
    
    public boolean isClosing() {
        return closing;
    }
    
    public boolean isReady() {
        return ready;
    }
    
    public BlockRegistry getBlockRegistry() {
        return registry;
    }
}
