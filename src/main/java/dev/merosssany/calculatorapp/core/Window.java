package dev.merosssany.calculatorapp.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

public class Window {
    private long window = 0;
    private String title = "";
    private int width;
    private int height;

    public Window(int width, int height, String title) {
        this.height = height;
        this.width = width;
        this.title = title;
        initGLFW();
    }

    private void initGLFW() {
        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(primaryMonitor);

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // Initially hidden

        this.window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        if (vidMode != null) {
            int centerX = (vidMode.width() - width) / 2;
            int centerY = (vidMode.height() - height) / 2;
            GLFW.glfwSetWindowPos(window, centerX, centerY);
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);

        // Enable v-sync
        GLFW.glfwSwapInterval(1);

        // Show the window
        GLFW.glfwShowWindow(window);
    }

    public void initOpenGL() {
        GLCapabilities capabilities = GL.createCapabilities();
    }

    public long getWindow() {
        return window;
    }
}
