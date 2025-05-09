package dev.merosssany.calculatorapp.core;

import javax.swing.*;

import static org.lwjgl.glfw.GLFW.*;

public class CleanupManager {

    private static boolean glfwInitialized = false; // Flag to track initialization

    public static void initGLFW() {
        if (!glfwInitialized) {
            if (!glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }
            glfwInitialized = true;
        }
    }

    public static void exit(int code) {
        if (code != 0) {
            createPopup("Application exit with code: "+code);
        } else {
            cleanup();
        }
        terminateGLFW();
        System.exit(code);
    }

    private static void cleanup() {
        terminateGLFW();
    }

    public static void terminateGLFW() {
        if (glfwInitialized) {
            glfwTerminate();
            glfwSetErrorCallback(null).free(); // Also free the error callback
            glfwInitialized = false;
        }
    }

    public static void createPopup(String errorMessage) {
        JOptionPane.showMessageDialog(
                null, // Parent component (can be null for a default window)
                errorMessage,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}