package org.infinitytwo.umbralore.core;

import org.infinitytwo.umbralore.block.*;
import org.infinitytwo.umbralore.core.constants.Constants;
import org.infinitytwo.umbralore.core.data.*;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.core.event.state.WindowResizedEvent;
import org.infinitytwo.umbralore.core.exception.IllegalChunkAccessException;
import org.infinitytwo.umbralore.core.logging.Logger;
import org.infinitytwo.umbralore.core.manager.CrashHandler;
import org.infinitytwo.umbralore.core.manager.ScreenManager;
import org.infinitytwo.umbralore.core.manager.WorkerThreads;
import org.infinitytwo.umbralore.core.manager.World;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.network.client.ClientNetworkThread;
import org.infinitytwo.umbralore.core.network.modern.ClientNetwork;
import org.infinitytwo.umbralore.core.network.modern.Packets;
import org.infinitytwo.umbralore.core.network.modern.ServerNetwork;
import org.infinitytwo.umbralore.core.network.server.ServerNetworkThread;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.registry.DimensionRegistry;
import org.infinitytwo.umbralore.core.renderer.*;
import org.infinitytwo.umbralore.core.ui.component.Scale;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.input.Button;
import org.infinitytwo.umbralore.core.ui.input.TextInput;
import org.infinitytwo.umbralore.core.ui.position.Anchor;
import org.infinitytwo.umbralore.core.ui.position.Pivot;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.dimension.Overworld;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static final Logger logger = new Logger(Main.class);
    private static FontRenderer fontRenderer;
    private static Window window;
    private static UIBatchRenderer renderer;
    private static TextBatchRenderer textRenderer;
    private static Screen screen;
    
    private static ServerThread server;
    private static TextureAtlas atlas;
    private static Environment env;
    private static Camera camera;
    private static GridMap map;
    private static Outline outliner;
    private static double delta;
    private static InputManager input;
    private static Screen mainScreen;
    private static World world;
    private static EventBus clientEventBus;
    private static boolean started;
    private static ServerNetwork serverNetwork;
    private static ClientNetwork clientNetwork;
    private static Overworld overworld;
    
    public static void main(String[] args) {
        // Early Setup
        earlySetup();
        // Construction
        construction();
        // Initialization
        init();
        double lastTime = glfwGetTime();
        // Render Loop
        while (!window.isShouldClose()) {
            double current = glfwGetTime();
            delta = current - lastTime;
            lastTime = current;
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            WorkerThreads.run();
            render();
            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }
        cleanup();
    }
    
    private static void earlySetup() {
        GLFWErrorCallback.createPrint(System.err).set();
        CrashHandler.init();
        
        window = new Window(1024, 512, "Umbralore: Test Run");
        logger.info("Early Setup");
        window.initOpenGL();
        window.setBackgroundColor(RGBA.fromRGBA(11,27,69,0));
    }
    
    private static void construction() {
        // IMPORTANT CONSTRUCTION
        EventBus.connect(Main.class);
        fontRenderer = new FontRenderer(Constants.fontFilePath, 32);
        textRenderer = new TextBatchRenderer(fontRenderer, 1);
        renderer = new UIBatchRenderer();
        screen = new Screen(renderer, window);
        logger.info("Constructing...");
        window.setWindowIcon("src/main/resources/assets/icon/icon.png");
        
        server = new ServerThread(894653);
        env = new Environment();
        camera = new Camera();
        map = new GridMap(BlockRegistry.getMainBlockRegistry());
        outliner = new Outline();
        input = new InputManager(window);
        clientEventBus = new EventBus("Client");
        
        // NETWORK
        clientNetwork = new ClientNetwork("127.0.0.1",5896,4789,(packet, connection) -> {
            System.out.println("RECEIVED PACKET "+packet.getClass().getSimpleName());
            if (packet instanceof Packets.PCommandData data) {
                String fullCommand = data.command();
                String[] args = fullCommand.split("\\s+");
                String command = args[0];
                
            } else if (packet instanceof Packets.PCommand data) {
                String[] args = data.command().split("\\s+");
                String command = args[0];
                
                if (command.equals("dimension")) {
                    System.out.println(args[1]);
                    World.getInstance().setCurrent(DimensionRegistry.getRegistry().get(args[1]));
                }
            } else if (packet instanceof Packets.PChunk chunkP) {
                int chunkX = chunkP.x();
                int chunkZ = chunkP.y();
                System.out.println("Reading chunk ("+chunkX+" "+chunkZ+")");
                
                ChunkData chunkData = ChunkData.of(chunkX,chunkZ,chunkP.blocks());
                
                WorkerThreads.dispatch(() -> {
                    Chunk chunk = Chunk.of(chunkData, World.getInstance().getMap(), World.getInstance().getTextureAtlas(), BlockRegistry.getMainBlockRegistry());
                    World.getInstance().getMap().addChunk(chunk);
                    System.out.println("CHUNK HAS BEEN CREATED: "+chunk.getPosition()); // NEVER PRINTED
                });
            }
        });
        
        // SCREENS
        mainScreen = new Screen(renderer,window);
        world = World.getInstance();
        overworld = new Overworld(1,BlockRegistry.getMainBlockRegistry());
    }
    
    private static void init() {
        Display.init();
        Packets.register();
        Display.onWindowResize(new WindowResizedEvent(window));
        DimensionRegistry.getRegistry().register(overworld);
        
        // Testing Here:
        // ----------------------------
        // INITIALIZATION
        BlockRegistry registry = BlockRegistry.getMainBlockRegistry();
        atlas = new TextureAtlas(486,22);
        
        try {
            registry.register(new GrassBlockType(atlas.addTexture("src/main/resources/grass_side.png", true)));
            registry.register(new DirtBlockType(atlas.addTexture("src/main/resources/dirt.png", true)));
            registry.register(new StoneBlockType(atlas.addTexture("src/main/resources/stone.png", true)));
            registry.register(new BedrockBlockType(atlas.addTexture("src/main/resources/pick.png", true)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        atlas.build();
        world.prepareForConnection(clientNetwork,server,atlas);
        World.setLocation(new SpawnLocation(new Vector3f(), overworld));
        world.setMap(map);
        
        Path fontPath = Path.of(Constants.fontFilePath);
        Button play = new Button(mainScreen, fontPath,"Start the Test"){
            
            @Override
            public void onMouseClicked(MouseButtonEvent e) {
                if (!started) {
                    server.start();
                }
                
                world.connectToServer();
                started = true;
                ScreenManager.popScreen();
            }
        };
        play.setSize(512,128);
        play.setPosition(new Anchor(0.5f,0.5f),new Pivot(0.5f,0.5f));
        play.setBackgroundColor(new RGBA(0,0,0,1));
        
        TextInput i = new TextInput(screen, fontPath) {
            @Override
            public void submit(String data) {
                clientNetwork.send(new Packets.PCommand(data),true);
            }
        };
        i.setPosition(new Anchor(0,0.5f),new Pivot(0,0.5f));
        i.addComponent(new Scale(1,-1,i));
        i.setHeight(128);
        i.setBackgroundColor(0,0,0,0.5f);
        i.setText("HELLO");
        i.setTextPosition(new Anchor(),new Pivot());
        
        mainScreen.register(play);
        
        ScreenManager.register("main",mainScreen);
        ScreenManager.setScreen("main");
    }
    
    private static void handleInput(boolean locked) {
        if (!locked) {
            if (input.isKeyPressed(GLFW_KEY_W)) camera.moveForward((float) delta);
            if (input.isKeyPressed(GLFW_KEY_S)) camera.moveBackward((float) delta);
            if (input.isKeyPressed(GLFW_KEY_A)) camera.moveLeft((float) delta);
            if (input.isKeyPressed(GLFW_KEY_D)) camera.moveRight((float) delta);
            if (input.isKeyPressed(GLFW_KEY_LEFT)) camera.rotate((float) (-delta) * 50, 0);
            if (input.isKeyPressed(GLFW_KEY_RIGHT)) camera.rotate((float) (delta) * 50, 0);
            if (input.isKeyPressed(GLFW_KEY_UP)) camera.rotate(0, (float) delta * 50);
            if (input.isKeyPressed(GLFW_KEY_DOWN)) camera.rotate(0, (float) -delta * 50);
            if (input.isKeyPressed(GLFW_KEY_SPACE)) camera.moveUp((float) delta);
            if (input.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) camera.moveDown((float) delta);
        }
    }
    
    private static void render() {
        if (!started) {
            Display.prepare2d();
            ScreenManager.draw();
        }
        input.update();
        handleInput(false);
        
        if (started) env.render(camera,window);
        world.draw(camera,window,5);
        camera.update((float) delta);
    }
    
    private static void getChunk(Vector2i pos) {
        BlockRegistry registry = BlockRegistry.getMainBlockRegistry();
        
        ChunkData data = server.getCurrentWorld().getChunkOrGenerate(pos); // From ServerProcedureGridMap
        for (int id : registry.getIds()) {
            if (registry.get(id) instanceof ServerBlockType) {
                throw new RuntimeException("Somehow registry is server-side...");
            }
        }
        if (data == null) return;
        
        Chunk chunk = null;
        try {
            chunk = Chunk.of(data, map, atlas, registry); // Passed ChunkData from ServerProcedureGridMap
        } catch (IllegalChunkAccessException ignored) {
        
        }
        map.addChunk(chunk);
    }
    
    public static void cleanup() {
        fontRenderer.cleanup();
        renderer.cleanup();
        window.cleanup();
        
        CleanupManager.exit(0);
    }
    
    public static Window getWindow() {
        return window;
    }
    
    public static FontRenderer getFontRenderer() {
        return fontRenderer;
    }
    
    public static UIBatchRenderer getUIBatchRenderer() {
        return renderer;
    }
    
    public static TextBatchRenderer getFontBatchRenderer() {
        return textRenderer;
    }
}
