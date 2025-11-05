package org.infinitytwo.umbralore.core;

import org.infinitytwo.umbralore.block.ServerBlockType;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.network.PacketReceived;
import org.infinitytwo.umbralore.core.logging.Logger;
import org.infinitytwo.umbralore.core.network.server.ServerNetworkThread;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.world.ServerGridMap;
import org.infinitytwo.umbralore.core.world.ServerProcedureGridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.infinitytwo.umbralore.core.world.dimension.Overworld;

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
    private ServerNetworkThread networkThread;
//    private NetworkHandler handler;
//    private NetworkCommandHandler commandHandler;
    private final Logger logger = new Logger(ServerThread.class);

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

        networkThread = new ServerNetworkThread(eventBus,5555);
//        handler = new ServerNetworkHandler(eventBus,networkThread,this);
//        commandHandler = new NetworkCommandHandler();
//
//        NetworkCommands.register(commandHandler);
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

        networkThread.start();
    }

    private void tick() {
        for (int id : registry.getIds()) {
            if (!(registry.get(id) instanceof  ServerBlockType)) throw new RuntimeException("E");
        }
        try {
            Thread.sleep(10); // To prevent high CPU usage
        } catch (InterruptedException e) {
            System.out.println("Hello?");
            cleanup();
        }
    }

    private void cleanup() {
        networkThread.shutdown();
        overworld.cleanup();
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

//    public NetworkHandler getNetworkHandler() {
//        return handler;
//    }

    public BlockRegistry getBlockRegistry() {
        return registry;
    }
    
    public ServerNetworkThread getNetworkThread() {
        return networkThread;
    }
}
