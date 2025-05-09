package dev.merosssany.calculatorapp;

import dev.merosssany.calculatorapp.core.*;
import dev.merosssany.calculatorapp.core.event.MouseHoverEvent;
import dev.merosssany.calculatorapp.core.io.InputHandler;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.ui.Button;
import dev.merosssany.calculatorapp.core.ui.InteractableUI;
import dev.merosssany.calculatorapp.core.ui.UI;
import dev.merosssany.calculatorapp.core.ui.font.CharacterInfo;
import dev.merosssany.calculatorapp.core.ui.font.FontMatrices;
import dev.merosssany.calculatorapp.core.ui.font.TextRenderer;
import dev.merosssany.calculatorapp.logging.Logger;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class Main {
    private static final Logger logger = new Logger("Main");
    private static Window window;

    public static void main(String[] args) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        logger.info("Launching game...");

        window = new Window(1024,512,"Hello");
        window.initOpenGL();

        InteractableUI t = new InteractableUI(new UIVector2Df(-1f,0f),2f,0.5f ,new RGBA(0, 255, 0,1f),window);
        UI topLeftHalfElement = new UI("Test", new UIVector2Df(-1f,1f),2f,0.5f ,new RGBA(255, 0, 0,0.9f));
        InputHandler handler = new InputHandler(window);

        topLeftHalfElement.init();
        t.init();
        handler.init();

        Shader shader = new Shader("assets/font/vertexShader.glsl", "assets/font/fragmentShader.glsl");

        int projectionUniform = shader.getUniformLocation("uProjection");
        int modelUniform = shader.getUniformLocation("uModel");
        int viewUniform = shader.getUniformLocation("uView");
        int textColorUniform = shader.getUniformLocation("uTextColor");
        int textureUniform = shader.getUniformLocation("uFontTexture");

        FontMatrices fontMatrices = new FontMatrices(window.getWidth(), window.getHeight(), true);
        Matrix4f projectionMatrix = fontMatrices.getProjectionMatrix();

        while (!glfwWindowShouldClose(window.getWindow())) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            t.draw();
            topLeftHalfElement.draw();

            EventBus.post(new MouseHoverEvent());

            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }

        window.cleanup();
        CleanupManager.exit(0);
    }

    public static void cleanup() {
        window.cleanup();
    }

    private static void tesst() {
        logger.info("Button Pressed");
    }
}