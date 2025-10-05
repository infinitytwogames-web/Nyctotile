package org.infinitytwo.umbralore.debug;

import com.moandjiezana.toml.Toml;
import org.infinitytwo.umbralore.*;
import org.infinitytwo.umbralore.block.*;
import org.infinitytwo.umbralore.data.ChunkData;
import org.infinitytwo.umbralore.data.PlayerData;
import org.infinitytwo.umbralore.entity.Player;
import org.infinitytwo.umbralore.event.SubscribeEvent;
import org.infinitytwo.umbralore.event.bus.EventBus;
import org.infinitytwo.umbralore.event.bus.LocalEventBus;
import org.infinitytwo.umbralore.event.input.CharacterInputEvent;
import org.infinitytwo.umbralore.event.input.KeyPressEvent;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.state.WindowResizedEvent; // ADDED: Import for the explicit resize call
import org.infinitytwo.umbralore.exception.IllegalChunkAccessExecption;
import org.infinitytwo.umbralore.logging.Logger;
import org.infinitytwo.umbralore.model.TextureAtlas;
import org.infinitytwo.umbralore.network.client.ClientNetworkThread;
import org.infinitytwo.umbralore.registry.BlockDataReader;
import org.infinitytwo.umbralore.registry.BlockRegistry;
import org.infinitytwo.umbralore.registry.ResourceManager;
import org.infinitytwo.umbralore.renderer.*;
import org.infinitytwo.umbralore.ui.Screen;
import org.infinitytwo.umbralore.ui.builtin.Background;
import org.infinitytwo.umbralore.ui.input.TextInput;
import org.infinitytwo.umbralore.ui.position.Anchor;
import org.infinitytwo.umbralore.ui.position.Pivot;
import org.infinitytwo.umbralore.world.GridMap;
import org.infinitytwo.umbralore.world.dimension.Overworld;
import org.joml.*;
import org.lwjgl.opengl.GL20;

import java.io.File;
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
    private static final Logger logger = new Logger("Umbralore");
    private static final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private static final HashMap<Integer, Boolean> keyStates = new HashMap<>();
    private static final HashMap<Integer, Boolean> lastKeyStates = new HashMap<>();
    private static final HashMap<Integer, Boolean> mouseStates = new HashMap<>();
    private static final HashMap<Integer, Boolean> lastMouseStates = new HashMap<>();

    private static Camera camera;
    private static GridMap map;
    private static FontRenderer textRenderer;
    private static BlockDataReader reader;
    private static ShaderProgram shaderProgram;
    public static TextureAtlas atlas;
    private static Environment env;
    private static Outline outliner;
    private static double delta;

    private static Vector3i pos;
    private static Overworld overworld;
    private static Chunk chunk;
    private static Toml toml;
    private static BlockRegistry registry = new BlockRegistry();
    private static ServerThread serverThread;
    private static ClientNetworkThread networkThread;
    private static LocalEventBus eventBus = new LocalEventBus("Network");
    private static boolean locked = false;
    private static UIBatchRenderer renderer;
    private static Screen pauseScreen;
    private static Player player;
    private static boolean setPosition;
    private static TextureAtlas itemAtlas;

    public static void main(String[] args) {
        Display.enable();
        toml = new Toml().read(new File("config.toml"));
        if (toml.getBoolean("debug", false)) {
//            logger.info("Creating Console");
//            new Console().setupRedirectedOutput();
        }

        Thread.currentThread().setName("Renderer Thread");
        CrashHandler crashHandler = new CrashHandler();
        crashHandler.buildText();
        crashHandler.init();
        logger.info("Starting up...");
        earlySetup();
        logger.info("Constructing Engine's classes...");
        construction();
        logger.info("Initializing Engine's classes...");
        init();
        logger.info("Initialization completed!");

        double lastTime = glfwGetTime();
        Mouse mouse = new Mouse();
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

            mouse.update(window.getWindowHandle());

            // --- GAME LOGIC GATED BY PAUSE STATE ---
            if (!locked) {
                // 1. Camera Look (Must be disabled when paused)
                camera.rotate((float) mouse.getDeltaX(), (float) -mouse.getDeltaY());

                // 2. Block Interaction (Must be disabled when paused)
                if (isMouseJustPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
                    GridMap.RaycastResult hit = map.raycast(camera.getPosition(), camera.getDirection(), 6);
                    if (hit != null) {
                        Vector3i face = hit.hitNormal();
                        Vector3i pos = new Vector3i(
                                hit.blockPos().x + face.x,
                                hit.blockPos().y + face.y,
                                hit.blockPos().z + face.z
                        );
                        if (map.getBlock(pos.x, pos.y, pos.z) == null) {
                            // FIX: Get the correct grass block ID from the registry.
                            Block block = new Block().create(Main.getBlockRegistry().get(1)); // 1 is the ID for grass
                            block.setPosition(pos.x, pos.y, pos.z);
                            try {
                                map.placeBlock(block);
                            } catch (IllegalChunkAccessExecption e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } else if (isMouseJustPressed(GLFW_MOUSE_BUTTON_LEFT)) {
                    GridMap.RaycastResult hit = map.raycast(camera.getPosition(), camera.getDirection(), 6);
                    if (hit != null) {
                        try {
                            map.removeBlock(hit.blockPos());
                        } catch (IllegalChunkAccessExecption e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            // --- END GATED LOGIC ---

            // 3. Player Movement (Gated inside handleInput now)
            handleInput(locked);

            render(alpha);

            glfwPollEvents();
            glfwSwapBuffers(window.getWindowHandle());
        }
        cleanup();
    }

    private static void applyPhysics(double fixedDelta) {
//        player.savePrevPosition();
        player.update((float) fixedDelta);
    }

    private static void earlySetup() {
        window = new Window(1000, 512, "Umbralore");
        Display.init();
        window.initOpenGL();
        EventBus.register(Main.class);

        Display.onWindowResize(new WindowResizedEvent(1000,512,window));
    }

    private static Screen setPos;

    private static void construction() {
        atlas = new TextureAtlas(2, 4);
        itemAtlas = new TextureAtlas(2, 5);

        registry = new BlockRegistry();
        textRenderer = new FontRenderer("src/main/resources/font.ttf", 16);

        renderer = new UIBatchRenderer();
        pauseScreen = new Screen(renderer, window);
        setPos = new Screen(renderer, window);
        TextInput input = new TextInput(textRenderer, setPos, new RGB(1, 1, 1)) {
            @Override
            public void submit(String data) {
                String[] pos = data.split(" ");
                Vector3f p = new Vector3f(Float.parseFloat(pos[0]), Float.parseFloat(pos[1]), Float.parseFloat(pos[2]));
                player.setPosition(p);
            }

            @SubscribeEvent
            @Override
            public void onMouseClicked(MouseButtonEvent e) {
                super.onMouseClicked(e);
            }

            @SubscribeEvent
            @Override
            public void onKeyPress(KeyPressEvent e) {
                super.onKeyPress(e);
            }

            @SubscribeEvent
            @Override
            public void onMouseClickedA(MouseButtonEvent e) {
                super.onMouseClickedA(e);
            }

            @SubscribeEvent
            @Override
            public void onCharacterPressed(CharacterInputEvent e) {
                super.onCharacterPressed(e);
            }
        };

        input.setPosition(new Anchor(0, 1), new Pivot(0, 0), new Vector2i(5, -255));
        input.setWidth(Display.width - 10);
        input.setHeight(250);
        input.setBackgroundColor(new RGBA(0, 0, 0, 0.5f));

        pauseScreen.register(new Background.Builder(renderer).applyDefault().backgroundColor(0, 0, 0, 0.5f).build());
        setPos.register(input);

        try {
            registry.register(new GrassBlockType(atlas.addTexture("src/main/resources/grass_side.png")));
            registry.register(new DirtBlockType(atlas.addTexture("src/main/resources/dirt.png")));
            registry.register(new StoneBlockType(atlas.addTexture("src/main/resources/stone.png")));
            registry.register(new BedrockBlockType(atlas.addTexture("src/main/resources/pick.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        atlas.build();

        reader = new BlockDataReader(registry);

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
    }

    private static void init() {
        outliner = new Outline(new ShaderProgram(
                """
                        #version 330 core
                        layout (location = 0) in vec3 position;
                        uniform mat4 model, view, projection;
                        void main() {
                            gl_Position = projection * view * model * vec4(position, 1.0);
                        }
                        """,
                """
                        #version 330 core
                        out vec4 FragColor;
                        uniform vec3 outlineColor;
                        void main() {
                            FragColor = vec4(outlineColor, 1.0);
                        }
                        """
        ));

        serverThread.start();
        networkThread.start();

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        map = new GridMap(registry);
//        new Generator().generateBiomeChunkHeightmap(
//                NoiseGenerationSettings.getDefault(
//                        561174,
//                        overworld.biomes
//                ),
//                new GridMap.ChunkPos(0,0),
//                overworld.biomes,
//                shaderProgram,
//                map,
//                atlas
//        );

        player = new Player(PlayerData.shell(""), map, camera, window);
        player.setPosition(0, 150, 0);

        ResourceManager.blocks = atlas;
        ResourceManager.items = itemAtlas;
    }


    private static boolean isEscKeyDown = false;

    private static void handleInput(boolean locked) {
        // [FIXED: Removed cursor management from here]

        // Only allow movement if UNPAUSED (!locked)
//        if (!locked) {
        if (isKeyPressed(GLFW_KEY_E)) {
            pauseGame();
        }
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
//        }
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

    private static void render(float alpha) {
        glEnable(GL_DEPTH_TEST);
        update();
        env.render(camera, window);
//        test.draw(camera, window, 5);

        map.draw(camera, window, 16);
        GridMap.RaycastResult hit = map.raycast(camera.getPosition(), camera.getDirection(), 6);
        if (hit != null) {

            outliner.render(new Vector3f(hit.blockPos()), camera, window, new Vector3f());
        }

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        window.getSize();
        Matrix4f ortho = new Matrix4f().ortho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        glViewport(0, 0, (int) window.getWidth(), (int) window.getHeight());

        Vector3f cameraPosition = camera.getPosition();
        textRenderer.renderText(cameraPosition.toString(), new Vector2i(0, 24), new RGB(1f, 1f, 1f));
        textRenderer.renderText(ortho, "Yaw: " + camera.getYaw() + " Pitch: " + camera.getPitch(), new Vector2i(0, 48), new RGB(1f, 1f, 1f));
        textRenderer.renderText(ortho, "POV: " + camera.getFov(), new Vector2i(0, 72), new RGB(1f, 1f, 1f));
        textRenderer.renderText(ortho, "FPS: " + Math.round(1 / delta), new Vector2i(0, 96), new RGB(1f, 1f, 1f));
        if (hit != null) {
            textRenderer.renderText(ortho, "Placement: " + new Vector3i(hit.blockPos().add(hit.hitNormal())), new Vector2i(0, 120), new RGB(1f, 1f, 1f));
            textRenderer.renderText(ortho, "Look At: " + hit.blockPos(), new Vector2i(0, 144), new RGB(1f, 1f, 1f));
            textRenderer.renderText(ortho, "Face: " + hit.hitNormal(), new Vector2i(0, 172), new RGB(1f, 1f, 1f));
            textRenderer.renderText(ortho, "Trying to place at: " + pos, new Vector2i(0, 196), new RGB(1f, 1f, 1f));
//            textRenderer.renderText(ortho, "Block Id: "+registry.getId(map.getBlock(hit.blockPos().x,hit.blockPos().y,hit.blockPos().z).getType().getId()), new Vector2i(0, 196+16), new RGB(1f,1f,1f));
        }

        camera.update((float) delta);
        player.handleInput((float) delta);
        player.draw();

        GL20.glUseProgram(0);
        Display.prepare2d();

        if (locked) {
            setPos.draw();
        }

        Display.prepare3d();
    }

    private static void update() {
        int chunkX = (int) Math.floor(camera.getPosition().x / ChunkData.SIZE_X);
        int chunkZ = (int) Math.floor(camera.getPosition().z / ChunkData.SIZE_Z);

        List<GridMap.ChunkPos> chunks = map.getMissingSurroundingChunks(
                new GridMap.ChunkPos(chunkX, chunkZ), 5
        );

        for (GridMap.ChunkPos pos : chunks) {
            getChunk(new Vector2i(pos.x(), pos.z()));
        }
    }

    private static void getChunk(Vector2i pos) {
        ChunkData data = serverThread.getCurrentWorld().getChunkOrGenerate(pos);
        for (int id : registry.getIds()) {
            if (registry.get(id) instanceof ServerBlockType) {
                throw new RuntimeException("Somehow registry is server-side...");
            }
        }
//        if (data == null) return;

        // This ensures all modifications to the GridMap are synchronized.
        dispatchTask(() -> {
            Chunk chunk = null;
            try {
                chunk = Chunk.of(data, map, shaderProgram, atlas, registry);
            } catch (IllegalChunkAccessExecption e) {

            }
            map.addChunk(chunk);
        });
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
            if (event.getKey() == GLFW_KEY_ESCAPE ||
                    event.getKey() == GLFW_KEY_E
            ) {
                locked = !locked;
            }
        } else keyStates.put(event.getKey(), event.getAction() == GLFW_PRESS || event.getAction() == GLFW_REPEAT);
    }

    public static boolean isKeyJustPressed(int key) {
        return keyStates.getOrDefault(key, false) && !lastKeyStates.getOrDefault(key, false);
    }

    public static boolean isMouseJustPressed(int button) {
        return mouseStates.getOrDefault(button, false) && !lastMouseStates.getOrDefault(button, false);
    }

    public static void dispatchTask(Runnable task) {
        tasks.add(task);
    }

    private static void runTasks() {
        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }
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
}
