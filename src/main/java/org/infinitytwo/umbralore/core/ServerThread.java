package org.infinitytwo.umbralore.core;

import org.infinitytwo.umbralore.block.ServerBlockType;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.data.PlayerData;
import org.infinitytwo.umbralore.core.data.SpawnLocation;
import org.infinitytwo.umbralore.core.entity.Player;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.network.PacketReceived;
import org.infinitytwo.umbralore.core.exception.UnknownRegistryException;
import org.infinitytwo.umbralore.core.logging.Logger;
import org.infinitytwo.umbralore.core.manager.Players;
import org.infinitytwo.umbralore.core.manager.World;
import org.infinitytwo.umbralore.core.network.modern.Packets;
import org.infinitytwo.umbralore.core.network.modern.ServerNetwork;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.registry.DimensionRegistry;
import org.infinitytwo.umbralore.core.security.AntiCheat;
import org.infinitytwo.umbralore.core.world.ServerProcedureGridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.infinitytwo.umbralore.core.world.dimension.Overworld;
import org.joml.Vector2i;
import org.joml.Vector3f;

public final class ServerThread extends Thread {
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
    private final Logger logger = new Logger(ServerThread.class);
    
    private final AntiCheat antiCheat = new AntiCheat(9);
    private ServerNetwork network;
    
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
        while (!closing) {
            tick();
        }
        logger.info("Closing!");
        cleanup();
    }

    private void construct() {
        seed = (int) Math.floor(Math.random() * 1000000);
        registry = new BlockRegistry();
        overworld = new Overworld(seed, registry);

        grass = new ServerBlockType("soil",false,"grass_block");
        dirt = new ServerBlockType("soil",false,"dirt");
        stone = new ServerBlockType("stone",false,"stone");
        mantle = new ServerBlockType("mantle",false,"mantle");

        registry.register(grass);
        registry.register(dirt);
        registry.register(stone);
        registry.register(mantle);
        
        World.setLocation(new SpawnLocation(new Vector3f(),overworld));
        DimensionRegistry.getRegistry().register(overworld);
        
        network = new ServerNetwork(5896,4789,((packet, connection) -> {
            // --- HANDLER: PCommand (Simple commands, e.g., 'getdimension') ---
            if (packet instanceof Packets.PCommand pCommand) {
                String line = pCommand.command().toLowerCase().trim();
                String[] args = line.split("\\s+");
                if (args.length == 0) return;
                logger.info("Got command: {}",line);
                String cmd = args[0];
                
                if (cmd.equals("getdimension")) {
                    Player player = Players.getPlayer(Players.getPlayerByConnection(connection));
                    if (player != null) {
                        network.send(new Packets.PCommand("dimension "+player.getDimension().getId()), connection, true);
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
                        if (antiCheat.isAcceptableVelocity(x, y, z, player)) {
                            player.setVelocity(x, y, z);
                        }
                    }
                } else if (cmd.startsWith("getchunk")) {
                    logger.info("Getting chunks....");
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
                        logger.info("Preparing for sending...");
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
                            int[] blocks = d.getWorld().getChunkOrGenerate(new Vector2i(x,y)).getBlockIds();
                            
                            // FIX 1: Instantiate the PChunk object and send it directly.
                            // The Network.send method will handle the encryption and wrapping.
                            network.send(new Packets.PChunk(x, y, blocks), connection, true);
                            
                        } catch (Exception e) {
                            logger.error("Error retrieving/generating chunk ({}, {}) in dimension {}.", x, y, dimension, e);
                            network.sendFailure(connection, "Server failed to load requested chunk.");
                        }
                        
                    } else {
                        network.sendFailure(connection,"Anti-Cheat system has detected suspicious activity from this client");
                        logger.warn("Anti-Cheat system has detected a violation at client \"{}\"",playerData.name());
                    }
                }
            }
        }));
    }

    @SubscribeEvent
    public void messageReceived(PacketReceived e) {
        logger.info(e.address.toString()+e.packet);
    }

    private void init() {
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                overworld.generate(new ChunkPos(x,y));
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
