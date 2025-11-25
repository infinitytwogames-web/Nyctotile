package org.infinitytwo.nyctotile.core;

import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.*;
import org.infinitytwo.nyctotile.core.event.state.WindowResizedEvent;
import org.infinitytwo.nyctotile.core.data.io.ResourceLoader;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Window {
    private long window = 0;
    private String title;
    private int width;
    private int height;
    private final Logger logger = LoggerFactory.getLogger(Window.class);
    
    private GLFWFramebufferSizeCallback framebufferSizeCallback;
    private GLFWKeyCallback glfwKeyCallback;
    private GLFWMouseButtonCallback glfwMouseButtonCallback;
    private GLFWCharCallback glfwCharCallback;
    private GLFWScrollCallback scrollCallback;
    private GLFWCursorPosCallback cursorPosCallback;
    
    public int getHeight() {
        return height;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        GLFW.glfwSetWindowTitle(window, title);
    }
    
    public int getWidth() {
        return width;
    }
    
    public Window(int width, int height, String title) {
        this.height = height;
        this.width = width;
        this.title = title;
        initGLFW();
    }
    
    private void initGLFW() {
        if (!glfwInit()) throw new IllegalStateException("Unable to initiate GLFW");
        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(primaryMonitor);
        
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        
        this.window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        Window instance = this;
        
        if (vidMode != null) {
            int centerX = (vidMode.width() - width) / 2;
            int centerY = (vidMode.height() - height) / 2;
            GLFW.glfwSetWindowPos(window, centerX, centerY);
        }
        
        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        
        // Enable v-sync
        GLFW.glfwSwapInterval(0);
        GLFW.glfwShowWindow(window);
        
        EventBus.connect(new WindowResizedEvent(width, height, this));
        
        glfwSetFramebufferSizeCallback(window, framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int newWidth, int newHeight) {
                width = newWidth;
                height = newHeight;
                EventBus.dispatch(new WindowResizedEvent(width, height, instance));
                GL11.glViewport(0, 0, width, height);
            }
        });
        
        glfwSetScrollCallback(window, scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long handle, double x, double y) {
                EventBus.dispatch(new MouseScrollEvent(instance, (int) x, (int) y));
            }
        });
        
        glfwSetKeyCallback(window, glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (isFocused()) EventBus.dispatch(new KeyPressEvent(key, action, mods));
            }
        });
        
        glfwSetMouseButtonCallback(window, glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (isFocused()) {
                    Vector2f p = getMousePosition();
                    EventBus.dispatch(new MouseButtonEvent(button, action, mods, p.x, p.y, instance));
                }
            }
        });
        
         glfwSetCharCallback(window, glfwCharCallback = new GLFWCharCallback() {
             @Override
             public void invoke(long handle, int codepoint) {
                 if (isFocused()) EventBus.dispatch(new CharacterInputEvent(codepoint, Character.toChars(codepoint)));
             }
        });
         
         glfwSetCursorPosCallback(window,cursorPosCallback = new GLFWCursorPosCallback() {
             @Override
             public void invoke(long window, double x, double y) {
                 EventBus.dispatch(new MouseCoordinatesEvent((float) x, (float) y));
             }
         });
    }
    
    public void initOpenGL() {
        GL.createCapabilities();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glViewport(0, 0, width, height);
    }
    
    public long getWindow() {
        return window;
    }
    
    public boolean isShouldClose() {
        return glfwWindowShouldClose(window);
    }
    
    public void cleanup() {
        if (glfwKeyCallback != null) glfwKeyCallback.free();
        if (glfwMouseButtonCallback != null) glfwMouseButtonCallback.free();
        if (glfwCharCallback != null) glfwCharCallback.free();
        if (framebufferSizeCallback != null) framebufferSizeCallback.free();
        if (scrollCallback != null) scrollCallback.free();
        if (cursorPosCallback != null) cursorPosCallback.free();
        
        GLFW.glfwDestroyWindow(window);
    }
    
    public static void terminateGLFW() {
        GLFW.glfwTerminate();
    }
    
    public Vector2f getMousePosition() {
        double[] cursorPositionX = new double[1];
        double[] cursorPositionY = new double[1];
        GLFW.glfwGetCursorPos(window, cursorPositionX, cursorPositionY);
        double mouseX = cursorPositionX[0];
        double mouseY = cursorPositionY[0];
        
        return new Vector2f((float) mouseX, (float) mouseY);
    }
    
    /**
     * Sets the window icon for the given GLFW window handle.
     *
     * @param iconPath
     *         The path to the icon image file (e.g., "assets/textures/icon.png").
     *         This path should be relative to your resources folder or project root.
     */
    public void setWindowIcon(String iconPath) {
        ByteBuffer imageBuffer;
        try (MemoryStack stack = MemoryStack.stackPush()) { // Use MemoryStack for temporary allocations
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1); // Number of components (e.g., 3 for RGB, 4 for RGBA) // BTW you see the "components" here. It did not count it
            
            // 1. Load the image into a ByteBuffer
            try {
                // Use the helper to load from classpath or filesystem
                imageBuffer = ResourceLoader.ioResourceToByteBuffer(iconPath, 4 * 1024); // 4KB initial buffer
            } catch (IOException e) {
                logger.error("Failed to load icon resource: " + iconPath, e);
                return;
            }
            
            // Load image using STBImage (expects 4 elements for RGBA) // BUT NOT THAT ONE 💀
            ByteBuffer decodedImage = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);
            if (decodedImage == null) {
                logger.error("Failed to decode image data for icon: " + iconPath + " - " + STBImage.stbi_failure_reason());
                return;
            }
            
            int width = w.get(0);
            int height = h.get(0);
            
            // 2. Prepare GLFWImage structure(s)
            // You can provide multiple icons (e.g., different sizes) for the OS to choose from.
            // For simplicity, we'll provide one.
            GLFWImage.Buffer icons = GLFWImage.malloc(1, stack); // Allocate space for 1 GLFWImage struct
            
            // Populate the GLFWImage struct
            icons.width(width);
            icons.height(height);
            icons.pixels(decodedImage); // The raw pixel data
            
            // 3. Set the window icon
            GLFW.glfwSetWindowIcon(window, icons);
            
            // 4. Clean up the decoded image data
            STBImage.stbi_image_free(decodedImage);
            
        } catch (Exception e) {
            logger.error("An error occurred while setting the window icon for: " + iconPath, e);
        }
    }
    
    public boolean isFocused() {
        return glfwGetWindowAttrib(window, GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }
    
    public long getWindowHandle() {
        return window;
    }
    
    public Vector2i getSize() {
        int[] width = new int[1];
        int[] height = new int[1];
        
        GLFW.glfwGetFramebufferSize(window, width, height);
        
        this.width = width[0];
        this.height = height[0];
        
        return new Vector2i(width[0], height[0]);
    }
    
    public void updateSize() {
        int[] width = new int[1];
        int[] height = new int[1];
        
        GLFW.glfwGetFramebufferSize(window, width, height);
        
        this.width = width[0];
        this.height = height[0];
    }
    
    public void close() {
        glfwSetWindowShouldClose(window, true);
    }
    
    public void setBackgroundColor(RGBA color) {
        glClearColor(color.red, color.green, color.blue, color.alpha);
    }
}