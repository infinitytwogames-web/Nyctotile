package dev.merosssany.calculatorapp;

import dev.merosssany.calculatorapp.core.Window;
import dev.merosssany.calculatorapp.logging.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    public static void main(String[] args) {
         GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        Logger logger = new Logger("Main");
        logger.log("Launching game...");

        Window window = new Window(700,450,"Hello");
        window.initOpenGL();

        while (!glfwWindowShouldClose(window.getWindow())) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer

            render();

            // --- END RENDERING ---

            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }
    }

    public static void render() {
        // Set the color for the display background (e.g., a lighter gray)
        glColor3f(0.9f, 0.9f, 0.9f);

        // Define the vertices of the display rectangle
        glBegin(GL_QUADS);
        glVertex2f(-0.8f, 0.8f);  // Top-left
        glVertex2f(0.8f, 0.8f);   // Top-right
        glVertex2f(0.8f, 0.6f);   // Bottom-right (making it a bit shorter)
        glVertex2f(-0.8f, 0.6f); // Bottom-left
        glEnd();
    }
}