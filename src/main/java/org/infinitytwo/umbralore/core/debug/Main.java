package org.infinitytwo.umbralore.core.debug;

import org.infinitytwo.umbralore.block.*;
import org.infinitytwo.umbralore.core.*;
import org.infinitytwo.umbralore.core.constants.Constants;
import org.infinitytwo.umbralore.core.constants.Material;
import org.infinitytwo.umbralore.core.data.*;
import org.infinitytwo.umbralore.core.data.buffer.NFloatBuffer;
import org.infinitytwo.umbralore.core.data.io.BlockDataReader;
import org.infinitytwo.umbralore.core.entity.Entity;
import org.infinitytwo.umbralore.core.entity.Player;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.input.KeyPressEvent;
import org.infinitytwo.umbralore.core.event.state.WindowResizedEvent; // ADDED: Import for the explicit resize call
import org.infinitytwo.umbralore.core.exception.IllegalChunkAccessException;
import org.infinitytwo.umbralore.core.data.Item;
import org.infinitytwo.umbralore.core.exception.IllegalDataTypeException;
import org.infinitytwo.umbralore.core.logging.Logger;
import org.infinitytwo.umbralore.core.manager.CrashHandler;
import org.infinitytwo.umbralore.core.manager.Mouse;
import org.infinitytwo.umbralore.core.manager.WorkerThreads;
import org.infinitytwo.umbralore.core.model.Model;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.model.builder.CubeModelBuilder;
import org.infinitytwo.umbralore.core.model.builder.ModelBuilder;
import org.infinitytwo.umbralore.core.network.client.ClientNetworkThread;
import org.infinitytwo.umbralore.core.registry.*;
import org.infinitytwo.umbralore.core.renderer.*;
import org.infinitytwo.umbralore.core.ui.builtin.InventoryGridViewer;
import org.infinitytwo.umbralore.core.ui.builtin.ItemSlot;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.builtin.Background;
import org.infinitytwo.umbralore.core.ui.builtin.Hotbar;
import org.infinitytwo.umbralore.core.ui.position.Anchor;
import org.infinitytwo.umbralore.core.ui.position.Pivot;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.dimension.Overworld;
import org.joml.*;
import org.lwjgl.opengl.GL20;

import javax.swing.*;
import java.io.IOException;
import java.lang.Math;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static Window window;
    private static final Logger logger = new Logger(Main.class);
    private static final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private static final HashMap<Integer, Boolean> keyStates = new HashMap<>();
    private static final HashMap<Integer, Boolean> lastKeyStates = new HashMap<>();
    private static final HashMap<Integer, Boolean> mouseStates = new HashMap<>();
    private static final HashMap<Integer, Boolean> lastMouseStates = new HashMap<>();

    private static Camera camera;
    private static GridMap map;
    private static FontRenderer textRenderer;
    private static ShaderProgram shaderProgram;
    public static TextureAtlas atlas;
    private static Environment env;
    private static Outline outliner;
    private static double delta;

    private static Overworld overworld;
    private static BlockRegistry registry = new BlockRegistry();
    private static ServerThread serverThread;
    private static ClientNetworkThread networkThread;
    private static EventBus eventBus = new EventBus("Network");
    private static boolean locked = false;
    private static UIBatchRenderer renderer;
    private static Screen pauseScreen;
    private static Player player;
    private static TextureAtlas itemAtlas;
    private static ItemRegistry itemRegistry;
    private static Screen mainScreen;
    private static EntityRenderer entityRenderer;
    private static Entity entity;
    private static BlockDataReader reader;
    private static int dynamic;

    public static void launchConsole() {
        // ⚠️ CRITICAL: Enforce that the Console setup runs on the EDT
        SwingUtilities.invokeLater(() -> {
            Console console = new Console(); // Constructor creates the JFrame components

            // Final JFrame commands must be here
            console.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            console.setLocation(100, 100);
            console.setSize(700, 500);

            // This makes the window appear!
            console.setVisible(true);

            // Now that the UI is visible, start the *worker* thread for log consumption
            Thread consoleWorker = new Thread(console, "Console-Worker-Thread");
            consoleWorker.setDaemon(true);
            consoleWorker.start();
        });
    }

    public static void main(String[] args) {
        Display.enable();
        Display.init();

        Thread.currentThread().setName("Renderer Thread");
        CrashHandler.init();
        
        logger.info("Starting up...");
        earlySetup();
        logger.info("Constructing Engine's classes...");
        construction();
        logger.info("Initializing Engine's classes...");
        init();
        logger.info("Initialization completed!");

        double lastTime = glfwGetTime();
        locked = false; // Initial state: Locked (Cursor visible, Menu often shows on start)
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        while (!glfwWindowShouldClose(window.getWindowHandle())) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            runTasks();

            double current = glfwGetTime();
            delta = current - lastTime;
            lastTime = current;

            accumulator += delta;

            // --- fixed-step physics loop (run here, not in render) ---
            while (accumulator >= fixedDelta) {
                applyPhysics(fixedDelta);
                accumulator -= fixedDelta;
            }

            // --- interpolation factor for rendering ---
            float alpha = (float) (accumulator / fixedDelta);

            // --- ESC TOGGLE LOGIC (Kept as is, it's correct) ---
            boolean escIsDown = glfwGetKey(window.getWindowHandle(), GLFW_KEY_ESCAPE) == GLFW_PRESS;

            if (escIsDown && !isEscKeyDown) {
                pauseGame();
            }
            isEscKeyDown = escIsDown;
            // ----------------------------------------------------

            // Copy current -> last
            lastKeyStates.clear();
            lastKeyStates.putAll(keyStates);

            for (int key = GLFW_KEY_SPACE; key <= GLFW_KEY_LAST; key++) {
                keyStates.put(key, glfwGetKey(window.getWindowHandle(), key) == GLFW_PRESS);
            }

            lastMouseStates.clear();
            lastMouseStates.putAll(mouseStates);

            for (int button = GLFW_MOUSE_BUTTON_1; button <= GLFW_MOUSE_BUTTON_LAST; button++) {
                mouseStates.put(button, glfwGetMouseButton(window.getWindowHandle(), button) == GLFW_PRESS);
            }

            // --- GAME LOGIC GATED BY PAUSE STATE ---
            if (!locked) {
                // 1. Camera Look (Must be disabled when paused)
                camera.rotate((float) Mouse.getDeltaX(), (float) -Mouse.getDeltaY());

                // 2. Block Interaction (Must be disabled when paused)
                if (isMouseJustPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
                    handleBlockInteraction();
                } else if (isMouseJustPressed(GLFW_MOUSE_BUTTON_LEFT)) {
                    RaycastResult hit = map.raycast(camera.getPosition(), camera.getDirection(), 6);
                    if (hit != null) {
                        try {
                            map.removeBlock(hit.blockPos());
                        } catch (IllegalChunkAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            
            handleInput(locked);
            render();

            glfwPollEvents();
            glfwSwapBuffers(window.getWindowHandle());
        }
        cleanup();
    }

    private static void handleBlockInteraction() {
        RaycastResult hit = map.raycast(camera.getPosition(), camera.getDirection(), 6);
        if (hit != null) {
            Vector3i face = hit.hitNormal();
            Vector3i pos = new Vector3i(
                    hit.blockPos().x + face.x,
                    hit.blockPos().y + face.y,
                    hit.blockPos().z + face.z
            );
            if (map.getBlock(pos.x, pos.y, pos.z) == null) {
                // FIX: Get the correct grass block ID from the registry.
                Block block = new Block(Main.getBlockRegistry().get(dynamic)); // 1 is the ID for grass
                block.setPosition(pos.x, pos.y, pos.z);
                try {
                    map.placeBlock(block);
                } catch (IllegalChunkAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void applyPhysics(double fixedDelta) {
        player.update((float) fixedDelta);
        entity.update((float) fixedDelta);
    }

    private static void earlySetup() {
        window = new Window(1000, 512, "Umbralore");
        Display.init();
        window.initOpenGL();
        EventBus.connect(Main.class);

        Display.onWindowResize(new WindowResizedEvent(1000, 512, window));
    }

    private static Screen setPos;

    private static void construction() {
        atlas = new TextureAtlas(2, 4);

        registry = BlockRegistry.getMainBlockRegistry();
        textRenderer = new FontRenderer("src/main/resources/font.ttf", 16);

        renderer = new UIBatchRenderer();

        try {
            registry.register(new GrassBlockType(atlas.addTexture("src/main/resources/grass_side.png", true)));
            registry.register(new DirtBlockType(atlas.addTexture("src/main/resources/dirt.png", true)));
            registry.register(new StoneBlockType(atlas.addTexture("src/main/resources/stone.png", true)));
            registry.register(new BedrockBlockType(atlas.addTexture("src/main/resources/pick.png", true)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        atlas.build();

        overworld = new Overworld(486486, registry);

        shaderProgram = new ShaderProgram(
                """
                        #version 330 core
                        layout (location = 0) in vec3 aPos;
                        layout (location = 1) in vec2 aTexCoord;
                        layout (location = 2) in float aBrightness;
                        
                        out vec2 TexCoord;
                        out float Brightness;
                        
                        uniform mat4 model;
                        uniform mat4 view;
                        uniform mat4 projection;
                        
                        void main() {
                            TexCoord = aTexCoord;
                            Brightness = aBrightness;
                            gl_Position = projection * view * model * vec4(aPos, 1.0);
                        }
                        """,
                """
                        #version 330 core
                        in vec2 TexCoord;
                        in float Brightness;
                        
                        uniform sampler2D ourTexture;
                        
                        out vec4 FragColor;
                        
                        void main() {
                            vec4 texColor = texture(ourTexture, TexCoord);
                            FragColor = vec4(texColor.rgb * Brightness, texColor.a);
                        }
                        """
        );

        env = new Environment();
        serverThread = new ServerThread(84653);
        camera = new Camera();
//        textRenderer = new FontRenderer("src/main/resources/font.ttf",16);

        try {
            networkThread = new ClientNetworkThread(eventBus, 5000, "localhost", 5555);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        org.infinitytwo.umbralore.core.data.ItemType
                i = new org.infinitytwo.umbralore.core.data.ItemType.Builder()
                .material(Material.GRASS)
                .type(Item.ItemBehaviour.ITEM)
                .name("")
                .build();

        try {
            itemRegistry = ItemRegistry.getMainRegistry();
            itemAtlas = ItemRegistry.getTextureAtlas();
            itemRegistry.register(i, itemAtlas.addTexture("src/main/resources/pickaxe.png", false));

            itemAtlas.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        map = new GridMap(registry);

        player = new Player(PlayerData.shell(""), overworld, camera, window);
        player.setPosition(0, 150, 0);
        player.adjust();
        player.getInventory().set(0, Item.of(i));
        player.getInventory().set(6, Item.of(i));

        // "PAUSE" SCREEN
        pauseScreen = new Screen(renderer, window);

        InventoryGridViewer viewer = new InventoryGridViewer(pauseScreen, new FontRenderer(Constants.fontFilePath, 16), window, (InventoryGridViewer.Factory) (slot, item, screen, fontRenderer, window) -> {
            ItemSlot ie = new ItemSlot(screen,fontRenderer,window);
            ie.setAtlas(itemAtlas);
            ie.setBackgroundColor(0,0,0,1);
            return ie;
        }, 9);
        viewer.linkInventory(player.getInventory());
        viewer.setAtlas(itemAtlas);
        viewer.setCellSize(128);
        viewer.setBackgroundColor(1,1,1,0.5f);
        viewer.setPosition(new Anchor(0.5f,0.5f),new Pivot(0.5f,0.5f));
        viewer.updateSize();

        pauseScreen.register(viewer);
        pauseScreen.register(new Background.Builder(renderer).applyDefault().backgroundColor(0, 0, 0, 0.5f).build());

        mainScreen = new Screen(renderer, window);
        Hotbar hotbar = new Hotbar(mainScreen, textRenderer, window, 9);
        hotbar.setAtlas(itemAtlas);
        hotbar.setCellSize(128);
        hotbar.setBackgroundColor(0, 0, 0, 0.5f);
        hotbar.setPosition(new Anchor(0.5f, 1), new Pivot(0.5f, 1));
        hotbar.updateSize();
        hotbar.linkInventory(player.getInventory());
        mainScreen.register(hotbar);

        Mouse.init(itemAtlas, mainScreen, -1, textRenderer, window);

        // MODEL RENDERING TEST
        Model model = new Model("test");
        AABB box = new AABB(0, 0, 0, 1, 1, 1);
        new ModelBuilder(box).cube(model.getVerticesBuffer(), new float[]{0, 0, 1, 1});
        int index = ModelRegistry.register(model);

        entity = new Entity("item", window, overworld, new Inventory(0), box) {
            @Override
            public Entity newInstance() {
                return null;
            }
        };
        entity.setModelIndex(index);

        entityRenderer = new EntityRenderer(atlas);
        entityRenderer.insert(entity);

        // BLOCK READING TEST
        BlockType type = new BlockType("", false, "", 0) {
            @BlockRegistry.Property
            public int data = 5;

            @Override
            public void buildModel(GridMap gridMap, int x, int y, int z, TextureAtlas atlas, BlockRegistry registry, NFloatBuffer buffer) {
                CubeModelBuilder.standardVerticesList(map, x, y, z, atlas.getUVCoords(2), buffer);
            }
        };

        try {
            dynamic = registry.registerDynamicBlock(type);
        } catch (IllegalDataTypeException e) {
            throw new RuntimeException(e);
        }

        reader = new BlockDataReader(registry);
    }

    private static void init() {
        outliner = new Outline();

        serverThread.start();
        networkThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static boolean isEscKeyDown = false;

    private static void handleInput(boolean locked) {
        if (!locked) {
            if (isKeyPressed(GLFW_KEY_W)) camera.moveForward((float) delta);
            if (isKeyPressed(GLFW_KEY_S)) camera.moveBackward((float) delta);
            if (isKeyPressed(GLFW_KEY_A)) camera.moveLeft((float) delta);
            if (isKeyPressed(GLFW_KEY_D)) camera.moveRight((float) delta);
            if (isKeyPressed(GLFW_KEY_LEFT)) camera.rotate((float) (-delta) * 50, 0);
            if (isKeyPressed(GLFW_KEY_RIGHT)) camera.rotate((float) (delta) * 50, 0);
            if (isKeyPressed(GLFW_KEY_UP)) camera.rotate(0, (float) delta * 50);
            if (isKeyPressed(GLFW_KEY_DOWN)) camera.rotate(0, (float) -delta * 50);
            if (isKeyPressed(GLFW_KEY_SPACE)) camera.moveUp((float) delta);
            if (isKeyPressed(GLFW_KEY_LEFT_SHIFT)) camera.moveDown((float) delta);
        }
    }

    // This method handles the state flip and cursor management correctly.
    private static void pauseGame() {
        locked = !locked;

        if (locked) { // Now PAUSED: Show cursor
            glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        } else { // Now UNPAUSED: Hide/lock cursor
            glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }
    }

    private static double fixedDelta = 1.0 / 60.0, // 60Hz physics
            accumulator = 0.0;

    private static void render() {
        Mouse.update();
        glEnable(GL_DEPTH_TEST);
        update();
        env.render(camera, window);
//        test.draw(camera, window, 5);

        map.draw(camera, window, 16);
        RaycastResult hit = map.raycast(camera.getPosition(), camera.getDirection(), 6);
        if (hit != null) {

            outliner.render(new Vector3f(hit.blockPos()), camera, window, new Vector3f());
        }

        window.getSize();
        Matrix4f ortho = new Matrix4f().ortho(0, window.getWidth(), window.getHeight(), 0, -1, 1);

        textRenderer.renderText(ortho, player.getPosition().toString(), new Vector2i(0, 24), new RGB(1f, 1f, 1f));
        textRenderer.renderText(ortho, player.getVelocity().toString(), new Vector2i(0, 2 + 24 * 2), new RGB(1f, 1f, 1f));

        camera.update((float) delta);

        GL20.glUseProgram(0);
        Display.prepare2d();

        mainScreen.draw();
        if (locked) pauseScreen.draw();

        Display.prepare3d();
    }

    private static void update() {
        int chunkX = (int) Math.floor(camera.getPosition().x / ChunkData.SIZE);
        int chunkZ = (int) Math.floor(camera.getPosition().z / ChunkData.SIZE);

        List<ChunkPos> chunks = map.getMissingSurroundingChunks(
                new ChunkPos(chunkX, chunkZ), 5
        );

        for (ChunkPos pos : chunks) {
            getChunk(new Vector2i(pos.x(), pos.z()));
        }
    }

    private static void getChunk(Vector2i pos) {
        ChunkData data = serverThread.getCurrentWorld().getChunkOrGenerate(pos); // From ServerProcedureGridMap
        for (int id : registry.getIds()) {
            if (registry.get(id) instanceof ServerBlockType) {
                throw new RuntimeException("Somehow registry is server-side...");
            }
        }
        if (data == null) return;

        // This ensures all modifications to the GridMap are synchronized.
        // Disabled temporary
//        dispatchTask(() -> {
        Chunk chunk = null;
        try {
            chunk = Chunk.of(data, map, shaderProgram, atlas, registry); // Passed ChunkData from ServerProcedureGridMap
        } catch (IllegalChunkAccessException e) {

        }
        map.addChunk(chunk);
//        });
    }

    protected static void cleanup() {
        window.cleanup();
        serverThread.shutdown();
        networkThread.shutdown();
    }

    public static boolean isKeyPressed(int keyCode) {
        return keyStates.getOrDefault(keyCode, false);
    }

    @SubscribeEvent
    public static void onKeyPress(KeyPressEvent event) {
        if (event.getAction() == GLFW_PRESS) {
            keyStates.put(event.getKey(), event.getAction() == GLFW_PRESS || event.getAction() == GLFW_REPEAT);
        }
    }

    private static void drop() {
        RaycastResult hit = map.raycast(camera.getPosition(), camera.getDirection(), 6);
        map.getChunk(GridMap.worldToChunk(hit.blockPos())).setData(GridMap.convertToLocalChunk(hit.blockPos()), reader.serialize(registry.get(dynamic), dynamic));
    }

    public static boolean isKeyJustPressed(int key) {
        return keyStates.getOrDefault(key, false) && !lastKeyStates.getOrDefault(key, false);
    }

    public static boolean isMouseJustPressed(int button) {
        return mouseStates.getOrDefault(button, false) && !lastMouseStates.getOrDefault(button, false);
    }

    public static void dispatchTask(Runnable task) {
        WorkerThreads.dispatch(task);
    }

    private static void runTasks() {
        WorkerThreads.run();
    }

    public static GridMap getGridMap() {
        return map;
    }

    public static ServerThread getServerThread() {
        return serverThread;
    }

    public static BlockRegistry getBlockRegistry() {
        return registry;
    }

    public static Screen getCurrentScreen() {
        return pauseScreen;
    }
}
