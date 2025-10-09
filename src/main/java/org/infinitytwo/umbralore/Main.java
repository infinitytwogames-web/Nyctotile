package org.infinitytwo.umbralore;

import org.infinitytwo.umbralore.constants.Constants;
import org.infinitytwo.umbralore.data.Inventory;
import org.infinitytwo.umbralore.data.ItemType;
import org.infinitytwo.umbralore.data.TextComponent;
import org.infinitytwo.umbralore.event.bus.EventBus;
import org.infinitytwo.umbralore.event.state.WindowResizedEvent;
import org.infinitytwo.umbralore.item.Item;
import org.infinitytwo.umbralore.logging.Logger;
import org.infinitytwo.umbralore.model.TextureAtlas;
import org.infinitytwo.umbralore.registry.ItemRegistry;
import org.infinitytwo.umbralore.registry.ResourceManager;
import org.infinitytwo.umbralore.renderer.*;
import org.infinitytwo.umbralore.ui.*;
import org.infinitytwo.umbralore.ui.builtin.Hotbar;
import org.infinitytwo.umbralore.ui.builtin.InventoryViewer;
import org.infinitytwo.umbralore.ui.builtin.ItemSlot;
import org.infinitytwo.umbralore.ui.component.ItemHolder;
import org.infinitytwo.umbralore.ui.component.TextureComponent;
import org.infinitytwo.umbralore.ui.position.Anchor;
import org.infinitytwo.umbralore.ui.position.Pivot;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static final Logger logger = new Logger("Main");
    private static FontRenderer fontRenderer;
    private static ConcurrentLinkedQueue<Runnable> queuedTasks;
    private static Window window;
    private static UI topLeftHalfElement;
    private static Matrix4f textProj;
    private static Label tets;
    private static UIBatchRenderer renderer;
    private static TextBatchRenderer textRenderer;
    private static Screen screen;

    private static final int VIRTUAL_UI_WIDTH = 1280;
    private static final int VIRTUAL_UI_HEIGHT = 720;
    public static TextProgressBar bar;

    public static void dispatchTask(Runnable task) {
        if (task == null) return;
        queuedTasks.add(task);
    }

    private static void runTasks() {
        Runnable task;
        while ((task = queuedTasks.poll()) != null) {
            task.run();
        }
    }

    public static void main(String[] args) {
        // Early Setup
        earlySetup();
        // Construction
        construction();
        // Initialization
        init();
        // Render Loop
        while (!glfwWindowShouldClose(window.getWindow())) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            runTasks(); // Most of them are reliant to OpenGL so here is suitable
            render();
            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }
        cleanup();
    }

    public static void tesst(String hmm) {
        logger.info(hmm);
    }

    private static void earlySetup() {
        GLFWErrorCallback.createPrint(System.err).set();

        window = new Window(1024, 512, "Hello");
        logger.info("Early Setup");
        window.initOpenGL();
    }

    private static void construction() {
        ShaderProgram program = new ShaderProgram(
                """
                        #version 330 core
                        // Layouts
                        layout (location = 0) in vec2 aPos;
                        layout (location = 1) in vec4 aColor;
                        layout (location = 2) in vec2 aTexturePos;
                        
                        // Uniform
                        uniform mat4 projection;
                        
                        // Output
                        out vec4 color;
                        out vec2 texCoords;
                        
                        void main() {
                            gl_Position = projection * vec4(aPos, 0.0, 1.0);
                            color = aColor;
                            texCoords = aTexturePos;
                        }
                        """,
                """
                        #version 330 core
                        // Input
                        in vec4 color;
                        in vec2 texCoords;
                        
                        // Uniform
                        uniform sampler2D uTexture;
                        uniform bool useTexture;
                        
                        // Output
                        out vec4 FragColor;
                        
                        void main() {
                            if (useTexture) {
                                FragColor = texture(uTexture, texCoords) * color;
                            } else {
                                FragColor = color;
                            }
                        }
                        """
        );
        // IMPORTANT CONSTRUCTION
        EventBus.register(Main.class);
        fontRenderer = new FontRenderer(Constants.fontFilePath,32);
        textRenderer = new TextBatchRenderer(fontRenderer, 1);
        renderer = new UIBatchRenderer();
        screen = new Screen(renderer,window);
        logger.info("Constructing...");
        window.setWindowIcon("src/main/resources/assets/icon/icon.png");
        queuedTasks = new ConcurrentLinkedQueue<>();
    }

    private static void init() {
        Display.init();
        Display.onWindowResize(new WindowResizedEvent(window));

        // Testing Here:
        ItemRegistry registry = ItemRegistry.getMainRegistry();
        TextureAtlas atlas = ItemRegistry.getTextureAtlas();
        ItemType type = new ItemType.Builder()
                .name(new TextComponent("E",new RGB(1,1,1)))
                .build()
        ;

        try {
            registry.register(type, atlas.addTexture("src/main/resources/pickaxe.png",false));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ResourceManager.items = atlas;
        Inventory inventory = new Inventory(9);
        inventory.set(0, Item.of(type));
        inventory.set(1, Item.of(type));
        inventory.set(2, Item.of(type));
        inventory.set(3, Item.of(type));
        inventory.set(4, Item.of(type));
        inventory.set(5, Item.of(type));

        InventoryViewer viewer = new InventoryViewer(screen,fontRenderer,3);
        viewer.setCellSize(128);
        viewer.linkInventory(inventory);

        screen.register(viewer);
        atlas.build();
    }

    private static void render() {
        screen.draw();
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