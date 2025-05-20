package dev.merosssany.calculatorapp;

import dev.merosssany.calculatorapp.core.*;
import dev.merosssany.calculatorapp.core.event.EventBus;
import dev.merosssany.calculatorapp.core.event.MouseHoverEvent;
import dev.merosssany.calculatorapp.core.io.InputHandler;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.render.CleanupManager;
import dev.merosssany.calculatorapp.core.render.Window;
import dev.merosssany.calculatorapp.core.ui.Button;
import dev.merosssany.calculatorapp.core.ui.UI;
import dev.merosssany.calculatorapp.core.ui.font.FontRenderer;
import dev.merosssany.calculatorapp.core.logging.Logger;
import dev.merosssany.calculatorapp.core.logging.LoggingLevel;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static final Logger logger = new Logger("Main");
    private static FontRenderer test;

    public static void main(String[] args) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        logger.info("Launching game...");

        Window window = new Window(1024, 512, "Hello");
        window.initOpenGL();
        Matrix4f textProj = new Matrix4f().ortho2D(0, window.getWidth(), window.getHeight(), 0);
        Matrix4f proj = new Matrix4f().ortho2D(-1f, 1f, -1f, 1f); // covers full NDC space


//        InteractableUI t = new InteractableUI(new UIVector2Df(-1f,0f),2f,0.5f ,new RGBA(0, 255, 0,1f), window);
        UI topLeftHalfElement = new UI("Test", new UIVector2Df(-1f,0f),2f,0.5f ,new RGBA(0f, 0, 1f,1f));
        InputHandler handler = new InputHandler(window);
//        ButtonSettings settings = new ButtonSettings(
//                Main::tesst,Main::tesst,0.02f,"",null
//        );

        Button button = null;
        try {
            button = new Button("Hello",1f,new RGBA(1f,0f,0f,1f),new UIVector2Df(0,0),2,0.5f,0f,new RGBA(1f,1f,1f,1f),window);
        } catch (IOException e) {
            CleanupManager.createPopup("Failed to create button\n"+logger.formatStacktrace(e,LoggingLevel.FATAL));
        }

        handler.init();
        topLeftHalfElement.init();
//        button.init();

//        glMatrixMode(GL_PROJECTION);
//        glLoadIdentity();
//        glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
//        glMatrixMode(GL_MODELVIEW);
//        glLoadIdentity();

        test = new FontRenderer("src/main/resources/fonts/Main.ttf",32);

        // Render Loop
        while (!glfwWindowShouldClose(window.getWindow())) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

//            t.draw();
            topLeftHalfElement.draw(proj);

            EventBus.post(new MouseHoverEvent());
            test.renderText(textProj,"Helljewhf ejwvhfiwehvwnhvgo",0,138,1,1,1);
//
//            assert font != null;
//            font.setColor(0.0f, 1.0f, 0.0f, 1.0f); // Green text
//            font.setScale(1.0f, 1.0f); // Normal size
//            font.renderText("Hello OpenGL", -0.9f, 0.8f, 2.0f / window.getWidth(), -2.0f / window.getHeight());

//            button.draw(proj);

            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }

        window.cleanup();
        CleanupManager.exit(0);
    }

    private static void tesst() {
        logger.info("Pressed");
    }
}