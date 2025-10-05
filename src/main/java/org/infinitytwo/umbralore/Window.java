package org.infinitytwo.umbralore;

import org.infinitytwo.umbralore.event.bus.EventBus;
import org.infinitytwo.umbralore.event.input.CharacterInputEvent;
import org.infinitytwo.umbralore.event.input.KeyPressEvent;
import org.infinitytwo.umbralore.event.input.MouseButtonEvent;
import org.infinitytwo.umbralore.event.state.WindowResizedEvent;
import org.infinitytwo.umbralore.io.ResourceLoader;
import org.infinitytwo.umbralore.logging.Logger;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_TRUE;

public class Window {
    private long window = 0;
    private String title;
    private int width;
    private int height;
    private final Logger logger = new Logger("Window");

    private GLFWFramebufferSizeCallback framebufferSizeCallback;
    private GLFWKeyCallback glfwKeyCallback;
    private GLFWMouseButtonCallback glfwMouseButtonCallback;
    private GLFWCharCallback glfwCharCallback;

    public int getHeight() {
        return height;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
        GLFW.glfwSetFramebufferSizeCallback(window, framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int newWidth, int newHeight) {
                width = newWidth;
                height = newHeight;
                EventBus.post(new WindowResizedEvent(width,height,instance));
                GL11.glViewport(0, 0, width, height);
            }
        });

        if (vidMode != null) {
            int centerX = (vidMode.width() - width) / 2;
            int centerY = (vidMode.height() - height) / 2;
            GLFW.glfwSetWindowPos(window, centerX, centerY);
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);

        // Enable v-sync
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);

//        EventBus.post(new WindowResizedEvent(width,height,this));

        glfwKeyCallback = GLFW.glfwSetKeyCallback(window, (windowHandle, key, scancode, action, mods) -> {
            if (isFocused()) EventBus.post(new KeyPressEvent(key, action, mods));
        });

        glfwMouseButtonCallback = GLFW.glfwSetMouseButtonCallback(window, (windowHandle, button, action, mods) -> {
            if (isFocused()) {
                Vector2f p = getMousePosition();
                EventBus.post(new MouseButtonEvent(button, action, mods,p.x, p.y, instance));
            }
        });

        glfwCharCallback = GLFW.glfwSetCharCallback(window, (windowHandle, codepoint) -> {
            if (isFocused()) EventBus.post(new CharacterInputEvent(codepoint, Character.toChars(codepoint)));
        });
    }

    public void initOpenGL() {
        GLCapabilities capabilities = GL.createCapabilities();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glViewport(0, 0, width, height);
    }

    public long getWindow() {
        return window;
    }

    public void cleanup() {
        if (glfwKeyCallback != null) glfwKeyCallback.free();
        if (glfwMouseButtonCallback != null) glfwMouseButtonCallback.free();
        if (glfwCharCallback != null) glfwCharCallback.free();
        if (framebufferSizeCallback != null) {
            framebufferSizeCallback.free();
        }

        GLFW.glfwDestroyWindow(window);
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
     * @param iconPath     The path to the icon image file (e.g., "assets/textures/icon.png").
     * This path should be relative to your resources folder or project root.
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

            // Load image using STBImage (expects 4 components for RGBA) // BUT NOT THAT ONE 💀
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
        // glfwGetWindowAttrib returns GLFW_TRUE (1) if focused, GLFW_FALSE (0) otherwise
        return glfwGetWindowAttrib(window, GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    public long getWindowHandle() {
        return window;
    }

    public Vector2i getSize() {
        int[] width = new int[1];
        int[] height = new int[1];

        GLFW.glfwGetFramebufferSize(window,width,height);

        this.width = width[0];
        this.height = height[0];

        return new Vector2i(width[0],height[0]);
    }

    public void updateSize() {
        int[] width = new int[1];
        int[] height = new int[1];

        GLFW.glfwGetFramebufferSize(window,width,height);

        this.width = width[0];
        this.height = height[0];
    }
}