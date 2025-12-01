package org.infinitytwo.nyctotile.core;

import org.infinitytwo.nyctotile.block.*;
import org.infinitytwo.nyctotile.core.constants.Constants;
import org.infinitytwo.nyctotile.core.data.RGBA;
import org.infinitytwo.nyctotile.core.data.io.InputManager;
import org.infinitytwo.nyctotile.core.data.world.ChunkData;
import org.infinitytwo.nyctotile.core.entity.Entity;
import org.infinitytwo.nyctotile.core.entity.Player;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.keyboard.KeyPressEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.event.input.VelocityChangedEvent;
import org.infinitytwo.nyctotile.core.event.state.WindowResizedEvent;
import org.infinitytwo.nyctotile.core.manager.*;
import org.infinitytwo.nyctotile.core.model.TextureAtlas;
import org.infinitytwo.nyctotile.core.network.thread.ClientNetwork;
import org.infinitytwo.nyctotile.core.network.data.Packets;
import org.infinitytwo.nyctotile.core.network.thread.ServerNetwork;
import org.infinitytwo.nyctotile.core.registry.BlockRegistry;
import org.infinitytwo.nyctotile.core.registry.DimensionRegistry;
import org.infinitytwo.nyctotile.core.renderer.*;
import org.infinitytwo.nyctotile.core.ui.Label;
import org.infinitytwo.nyctotile.core.ui.component.Scale;
import org.infinitytwo.nyctotile.core.ui.layout.Scene;
import org.infinitytwo.nyctotile.core.ui.layout.scroll.ScrollableMenu;
import org.infinitytwo.nyctotile.core.ui.input.Button;
import org.infinitytwo.nyctotile.core.ui.input.TextInput;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.infinitytwo.nyctotile.core.world.GMap;
import org.infinitytwo.nyctotile.core.world.GridMap;
import org.infinitytwo.nyctotile.core.world.dimension.Overworld;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.infinitytwo.nyctotile.core.Game.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static FontRenderer fontRenderer;
    private static Window window;
    private static UIBatchRenderer renderer;
    private static Scene scene;
    
    private static ServerThread server;
    private static TextureAtlas atlas;
    private static Environment env;
    private static Camera camera;
    private static GridMap map;
    private static Outline outliner;
    private static InputManager input;
    private static Scene mainScene;
    private static World world;
    private static EventBus clientEventBus;
    private static boolean started;
    private static ServerNetwork serverNetwork;
    private static ClientNetwork clientNetwork;
    private static Overworld overworld;
    private static final float fixedDelta = (float) 1 / 60;
    private static double accumulator;
    private static boolean locked;
    
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
            
            accumulator += delta;
            while (accumulator >= fixedDelta) {
                applyPhysics();
                accumulator -= fixedDelta;
            }
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            WorkerThreads.run();
            render();
            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }
        cleanup();
    }
    
    private static void applyPhysics() {
        if (world.getLocalPlayer() != null && world.getCurrent() != null &&
                world.getMap().getChunk(GMap.worldToChunk((int) world.getLocalPlayer().getPosition().x, (int) world.getLocalPlayer().getPosition().z)) != null)
            World.getInstance().getLocalPlayer().update(fixedDelta, map);
    }
    
    private static void earlySetup() {
        GLFWErrorCallback.createPrint(System.err).set();
        CrashHandler.init();
        
        window = new Window(1024, 512, NAME+": Test Run");
        logger.info("Early Setup");
        window.initOpenGL();
        window.setBackgroundColor(RGBA.fromRGBA(11, 27, 69, 0));
    }
    
    private static void construction() {
        // IMPORTANT CONSTRUCTION
        sensitivity = 0.5f;
        EventBus.connect(Main.class);
        fontRenderer = new FontRenderer(Constants.fontFilePath, 32);
        renderer = new UIBatchRenderer();
        scene = new Scene(renderer, window);
        logger.info("Constructing...");
        window.setWindowIcon("src/main/resources/assets/icon/icon.png");
        
        server = new ServerThread(894653);
        env = new Environment();
        camera = new Camera();
        map = new GridMap(BlockRegistry.getMainBlockRegistry());
        outliner = new Outline();
        input = new InputManager(window);
        clientEventBus = new EventBus();
        Mouse.setWindow(window);
        
        // NETWORK
        clientNetwork = new ClientNetwork("127.0.0.1", 5896, 4789, (packet, connection) -> {
            if (packet instanceof Packets.PCommandData data) {
                String fullCommand = data.command();
                String[] args = fullCommand.split("\\s+");
                
            } else if (packet instanceof Packets.PCommand data) {
                String[] args = data.command().split("\\s+");
                String command = args[0];
                
                if (command.equals("dimension")) {
                    System.out.println(args[1]);
                    world.setCurrent(DimensionRegistry.getRegistry().get(args[1]));
                }
            } else if (packet instanceof Packets.PChunk chunkP) {
                int chunkX = chunkP.x();
                int chunkZ = chunkP.y();
                
                ChunkData chunkData = ChunkData.of(chunkX, chunkZ, chunkP.blocks());
                
                WorkerThreads.dispatch(() -> {
                    Chunk chunk = Chunk.of(chunkData, World.getInstance().getMap(), World.getInstance().getTextureAtlas(), BlockRegistry.getMainBlockRegistry());
                    World.getInstance().getMap().addChunk(chunk);
//                    world.getRequested().remove(new ChunkPos(chunkX,chunkZ));
                });
            } else if (packet instanceof Packets.PPosition pos) {
                logger.info("GOT POSITION: X={}, Y={}, Z={}",
                        pos.x(), pos.y(), pos.z());
                
                // The LERP factor controls the smoothness (e.g., 0.2f corrects 20% of the distance each time).
                final float LERP_FACTOR = 0.3f;
                
                // --- 1. LOCAL PLAYER (Position Correction) ---
                if (pos.leastSignificant() == 0 || pos.mostSignificant() == 0) {
                    
                    // Target position from the server
                    Vector3f serverPos = new Vector3f(pos.x(), pos.y(), pos.z());
                    Entity player = world.getLocalPlayer();
                    Vector3f currentPos = player.getPosition();
                    
                    // Calculate distance squared to check for drift
                    float distSq = currentPos.distanceSquared(serverPos);
                    
                    if (distSq > 0.0001f) {
                        // Only apply LERP if there is noticeable drift
                        
                        // LERP calculation: current + (target - current) * factor
                        float newX = currentPos.x + (serverPos.x - currentPos.x) * LERP_FACTOR;
                        float newY = 0;
                        if (currentPos.y < serverPos.y + 1.5 && player.isGrounded()) newY = currentPos.y + (serverPos.y - currentPos.y) * LERP_FACTOR;
                        else newY = currentPos.y;
                        float newZ = currentPos.z + (serverPos.z - currentPos.z) * LERP_FACTOR;
                        
                        player.setPosition(newX, newY, newZ);
                        
                        if (distSq > 1.0f) {
                            logger.warn("Drift Detected: Correcting local position by LERP (Distance: {}).", Math.sqrt(distSq));
                        }
                    }
                    
                    // --- 2. REMOTE ENTITIES (Smooth Visualization) ---
                } else {
                    // This handles other entities (other players).
                    Entity entity = EntityManager.getEntityFromId(new UUID(pos.mostSignificant(), pos.leastSignificant()));
                    
                    if (entity != null) {
                        // Target position from the server
                        Vector3f serverPos = new Vector3f(pos.x(), pos.y(), pos.z());
                        Vector3f currentPos = entity.getPosition();
                        
                        // LERP calculation: current + (target - current) * factor
                        float newX = currentPos.x + (serverPos.x - currentPos.x) * LERP_FACTOR;
                        float newY = currentPos.y + (serverPos.y - currentPos.y) * LERP_FACTOR;
                        float newZ = currentPos.z + (serverPos.z - currentPos.z) * LERP_FACTOR;
                        
                        entity.setPosition(newX, newY, newZ);
                    }
                }
            }
        },"Dev","");
        
        // SCREENS
        mainScene = new Scene(renderer, window);
        world = World.getInstance();
        overworld = new Overworld(1, BlockRegistry.getMainBlockRegistry());
        world.setCurrent(overworld);
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
        atlas = new TextureAtlas(486, 22);
        
        try {
            registry.register(new GrassBlockType(atlas.addTexture("src/main/resources/grass_side.png", true)));
            registry.register(new DirtBlockType(atlas.addTexture("src/main/resources/dirt.png", true)));
            registry.register(new StoneBlockType(atlas.addTexture("src/main/resources/stone.png", true)));
            registry.register(new BedrockBlockType(atlas.addTexture("src/main/resources/pick.png", true)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        atlas.build();
        world.prepareForConnection(camera, window, clientNetwork, server, atlas);
        world.setMap(map);
        world.getLocalPlayer().setInputHandler(input);
        world.getLocalPlayer().getEventBus().register(new Object() {
            // We can reuse lastSentVelocity to hold the last sent position for throttling.
            private final Vector3f lastSentPosition = new Vector3f();
            private static final float EPSILON = 0.01f; // Tolerance for movement distance
            
            @SubscribeEvent
            public void e(VelocityChangedEvent e) {
                // VelocityChangedEvent is now a misnomer; it's triggered when the player moves.
                Player player = world.getLocalPlayer();
                Vector3f currentPosition = player.getPosition(); // Get the final, authoritative client position
                
                // Check for significant change in position
                if (Math.abs(currentPosition.x - lastSentPosition.x) > EPSILON ||
                        Math.abs(currentPosition.y - lastSentPosition.y) > EPSILON ||
                        Math.abs(currentPosition.z - lastSentPosition.z) > EPSILON) {
                    
                    // Send the authoritative position packet
                    clientNetwork.send(new Packets.PCommand(String.format("setposition %.3f %.3f %.3f",
                            currentPosition.x, currentPosition.y, currentPosition.z)), false);
                    
                    // Update the last-sent position
                    lastSentPosition.set(currentPosition);
                }
            }
        });
        
        Path fontPath = Path.of(Constants.fontFilePath);
        Button play = new Button(mainScene, fontPath, "Start the Test") {
            
            @Override
            public void onMouseClicked(MouseButtonEvent e) {
                if (!started) {
                    server.start();
                }
                
                glfwSetInputMode(window.getWindow(),GLFW_CURSOR,GLFW_CURSOR_DISABLED);
                world.connectToServer();
                started = true;
                SceneManager.popScreen();
            }
        };
        play.setSize(512, 128);
        play.setPosition(new Anchor(0.5f, 0.5f), new Pivot(0.5f, 0.5f));
        play.setBackgroundColor(new RGBA(0, 0, 0, 1));
        
        TextInput i = new TextInput(scene, fontPath) {
            @Override
            public void submit(String data) {
                clientNetwork.send(new Packets.PCommand(data), true);
            }
        };
        i.setPosition(new Anchor(0, 0.5f), new Pivot(0, 0.5f));
        i.addComponent(new Scale(1, -1, i));
        i.setHeight(128);
        i.setBackgroundColor(0, 0, 0, 0.5f);
        i.setText("HELLO");
        i.setTextPosition(new Anchor(), new Pivot());
        
        ScrollableMenu menu = new ScrollableMenu(
                mainScene, // Title/Header height in pixels
                window
        );
        
        // Set position and size of the ScrollableMenu UI
        menu.setPosition(new Anchor(0.5f,0.5f),new Pivot(0.5f,0.5f));
        menu.setSize(1024, 512);
        menu.setBackgroundColor(0,0,0,0.5f);
        
        menu.getScrollButton().setBackgroundColor(1,1,1,1);
        menu.getScrollButton().setWidth(50);
        
        // Add some UI elements to the scrollable content area
        for (int j = 0; j < 20; j++) {
            Label label = new Label(mainScene,fontPath);
            label.setSize(250,64);
            label.setBackgroundColor(0.25f,0.25f,0.25f,1);
            label.setText("Text: "+j);
            label.setOffset(0,64 * j);
            
            menu.addUI(label);
        }
        
        // Register menu itself to the scene
        mainScene.register(menu);
//        mainScene.register(play);
        
        SceneManager.register("main", mainScene);
        SceneManager.setScreen("main");
    }
    
    private static void handleInput() {
        if (Game.isLocked()) {
            camera.rotate((float) Mouse.getDeltaX() * sensitivity, (float) -Mouse.getDeltaY() * sensitivity);
            
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
            SceneManager.draw();
        } else
            fontRenderer.renderText(Display.get3DProjectionMatrix(camera,window),"Velocity: "+VectorMath.toString(VectorMath.toInt(world.getLocalPlayer().getVelocity())),0,32,0,0,0);
        input.update();
        handleInput();
        
        if (started) env.render(camera, window);
        world.draw(5);
        
        camera.update((float) delta);
        Mouse.update();
    }
    
    public static void cleanup() {
        fontRenderer.cleanup();
        renderer.cleanup();
        window.cleanup();
        
        System.exit(0);
    }
    
    public static Window getWindow() {
        return window;
    }
    
    @SubscribeEvent
    public static void onKeyPressed(KeyPressEvent e) {
        if (e.getAction() == GLFW_RELEASE) return;
        if (e.getKey() == GLFW_KEY_ESCAPE) {
            Game.pauseGame(window);
        }
    }
}
