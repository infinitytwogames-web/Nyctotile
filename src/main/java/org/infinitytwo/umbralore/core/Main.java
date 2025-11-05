package org.infinitytwo.umbralore.core;

import org.infinitytwo.umbralore.block.BedrockBlockType;
import org.infinitytwo.umbralore.block.DirtBlockType;
import org.infinitytwo.umbralore.block.GrassBlockType;
import org.infinitytwo.umbralore.block.StoneBlockType;
import org.infinitytwo.umbralore.core.constants.Biomes;
import org.infinitytwo.umbralore.core.constants.Constants;
import org.infinitytwo.umbralore.core.data.ChunkPos;
import org.infinitytwo.umbralore.core.debug.TerminalFrame;
import org.infinitytwo.umbralore.core.event.SubscribeEvent;
import org.infinitytwo.umbralore.core.event.bus.EventBus;
import org.infinitytwo.umbralore.core.event.network.PacketReceived;
import org.infinitytwo.umbralore.core.event.state.WindowResizedEvent;
import org.infinitytwo.umbralore.core.logging.Logger;
import org.infinitytwo.umbralore.core.network.NetworkHandler;
import org.infinitytwo.umbralore.core.network.client.ClientNetworkThread;
import org.infinitytwo.umbralore.core.network.server.ServerNetworkThread;
import org.infinitytwo.umbralore.core.registry.BlockRegistry;
import org.infinitytwo.umbralore.core.renderer.*;
import org.infinitytwo.umbralore.core.ui.*;
import org.infinitytwo.umbralore.core.ui.component.Scale;
import org.infinitytwo.umbralore.core.ui.display.Screen;
import org.infinitytwo.umbralore.core.ui.input.TextInput;
import org.infinitytwo.umbralore.core.ui.position.Anchor;
import org.infinitytwo.umbralore.core.ui.position.Pivot;
import org.infinitytwo.umbralore.core.world.ServerGridMap;
import org.infinitytwo.umbralore.core.world.ServerProcedureGridMap;
import org.infinitytwo.umbralore.core.world.generation.NoiseGenerationSettings;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static final Logger logger = new Logger(Main.class);
    private static final byte[] empty = {};
    private static FontRenderer fontRenderer;
    private static ConcurrentLinkedQueue<Runnable> queuedTasks;
    private static Window window;
    private static UIBatchRenderer renderer;
    private static TextBatchRenderer textRenderer;
    private static Screen screen;
    
    private static final int VIRTUAL_UI_WIDTH = 1280;
    private static final int VIRTUAL_UI_HEIGHT = 720;
    public static TextProgressBar bar;
    private static TerminalFrame teriminal;
    private static ServerNetworkThread serverThread;
    private static ClientNetworkThread clientThread;
    // Removed: private static PacketAssembly assembly;
    
    
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
        
        window = new Window(1024, 512, "Umbralore: Test Run");
        logger.info("Early Setup");
        window.initOpenGL();
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
        queuedTasks = new ConcurrentLinkedQueue<>();
    }
    
    private static void init() {
        Display.init();
        Display.onWindowResize(new WindowResizedEvent(window));
        
        // Testing Here:
        EventBus server = new EventBus("Test-Server");
        EventBus client = new EventBus("Test-Client");
        // Removed: assembly = new PacketAssembly();
        serverThread = new ServerNetworkThread(server, 5000);
        
        try {
            clientThread = new ClientNetworkThread(client, 5555, "127.0.0.1", 5000);
            
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        
        serverThread.start();
        clientThread.start();
        
        serverThread.offlineMode(true);
        
        TextInput input = new TextInput(screen, Path.of(Constants.fontFilePath), new RGB(1, 1, 1)) {
            // Forgot to tell you; There is a bug where when you make an inherited class,
            // Either Java removes the @SubscribeEvent or fails to find it
            @Override
            public void submit(String data) {
                command(data);
            }
        };
//        Label input = new Label(screen,fontRenderer,new RGB(1,1,1));
        
        input.setSize(1024, 128);
        input.addComponent(new Scale(1, -1, input));
        input.setPosition(new Anchor(0, 0.5f), new Pivot(0, 1));
        input.setBackgroundColor(0.25f, 0.25f, 0.25f, 1);
        input.setDisabled(false);
//        input.setText("Hello");
        
        screen.register(input);
        BlockRegistry.getMainBlockRegistry().register(new GrassBlockType(0));
        BlockRegistry.getMainBlockRegistry().register(new BedrockBlockType(0));
        BlockRegistry.getMainBlockRegistry().register(new DirtBlockType(0));
        BlockRegistry.getMainBlockRegistry().register(new StoneBlockType(0));
        
        // SERVER
        ServerProcedureGridMap gridMap = new ServerProcedureGridMap(5, NoiseGenerationSettings.getDefault(5, Biomes.getBiomes()),BlockRegistry.getMainBlockRegistry());
        gridMap.generate(new ChunkPos(0,0));
        server.register(new Object(){
            @SubscribeEvent
            public void e(PacketReceived e) {
                
                String cmd = new String(e.packet.payload(), StandardCharsets.UTF_8);
                System.out.println(cmd);
                if (cmd.startsWith("e")) {
                    System.out.println("Sending Chunks...");
                    // Send the chunk data, which will now be fragmented and reassembled by the client
                    serverThread.send(gridMap.getChunkOrGenerate(new Vector2i(0,0)).serialize(),e.packet.address(),e.packet.port(),e.packet.type(), true, true);
                }
            }
        });
        
        // CLIENT
    }
    
    private static boolean command(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || parts[ 0 ].isEmpty()) {
            return false; // No command entered
        }
        
        String cmd = parts[ 0 ].toLowerCase(); // The command (e.g., "post")
        
        // 2. Handle the "post" command
        if (cmd.equals("post")) {
            if (parts.length < 2) {
                System.out.println("Usage: post <subcommand> [args...]");
                return true; // Command was recognized, but usage was wrong
            }
            
            String subCmd = parts[ 1 ].toLowerCase();
            
            if (subCmd.equals("connect")) {
                System.out.println("-> AUTH: Sending connection request packet to 1.1.1.1...");
                clientThread.connect();
                return true;
            } else if (subCmd.equals("ping")) {
                clientThread.ping();
                System.out.println("Sent ping packet.");
                return true;
            } else if (subCmd.equals("auth")) {
                System.out.println("Authenticating...");
                clientThread.sendAuthentication();
                
            } else if (subCmd.equals("command")) {
                if (parts.length < 3) {
                    System.out.println("Usage: post command <data>");
                    return true;
                }
                // Reconstruct the command string for sending
                String commandData = command.substring(command.indexOf("command") + "command".length()).trim();
                clientThread.send(commandData,true,true);
                System.out.println("Sent command: " + commandData);
            } else {
                System.out.println("Unknown 'post' subcommand: " + subCmd);
                return true;
            }
        }
        
        // 3. Handle other top-level commands (e.g., "echo")
        if (cmd.equals("echo")) {
            // Reconstruct the rest of the string for the echo output
            String message = command.substring(parts[ 0 ].length()).trim();
            System.out.println(message);
            return true;
        }
        
        // 4. Command not recognized
        return false;
    }
    
    private static void render() {
        // Removed the incorrect reassembly loop from the render method
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
