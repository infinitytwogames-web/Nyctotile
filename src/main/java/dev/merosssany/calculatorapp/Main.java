package dev.merosssany.calculatorapp;

import dev.merosssany.calculatorapp.core.*;
import dev.merosssany.calculatorapp.core.discovery.CoreEngineLoader;
import dev.merosssany.calculatorapp.core.event.EventBus;
import dev.merosssany.calculatorapp.core.event.MouseHoverEvent;
import dev.merosssany.calculatorapp.core.event.stack.EventStack;
import dev.merosssany.calculatorapp.core.io.HoverEventRegister;
import dev.merosssany.calculatorapp.core.io.InputHandler;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.render.CleanupManager;
import dev.merosssany.calculatorapp.core.render.Window;
import dev.merosssany.calculatorapp.core.ui.Button;
import dev.merosssany.calculatorapp.core.ui.InteractableUI;
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
//    public static ;

    public static void main(String[] args) {
        EventStack.registerChannel("initial");

        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        Window window = new Window(1024, 512, "Hello");
        logger.info("Launching game...");
        window.initOpenGL();

        InputHandler handler = new InputHandler(window);
        HoverEventRegister hover = new HoverEventRegister(window);

        logger.info("Constructing....");
        Matrix4f textProj = new Matrix4f().ortho2D(0, window.getWidth(), window.getHeight(), 0);
        test = new FontRenderer("src/main/resources/fonts/Main.ttf",32);

        InteractableUI t = new InteractableUI(new UIVector2Df(-1f, 0f), 2f, 0.5f, new RGBA(0, 255, 0, 1f), window);
        UI topLeftHalfElement = new UI("Test", new UIVector2Df(-1f, 1f), 2f, 0.5f, new RGBA(0f, 0, 1f, 1f));

        Button button = null;
        try {
            button = new Button("Hello",1f,new RGBA(1f,0f,0f,1f),new UIVector2Df(0,0),2,0.5f,0f,new RGBA(1f,1f,1f,1f),window,textProj);
        } catch (IOException e) {
            CleanupManager.createPopup("Failed to create button\n"+logger.formatStacktrace(e, LoggingLevel.FATAL));
        }

        test = new FontRenderer("src/main/resources/fonts/Main.ttf",32);

        handler.init();
        topLeftHalfElement.init();
        t.init();
        // Render Loop
        while (!glfwWindowShouldClose(window.getWindow())) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            hover.update();
            topLeftHalfElement.draw();
            t.draw();

            EventBus.post(new MouseHoverEvent());
            test.renderText(textProj,"Helljewhf ejwvhfiwehvwnhvgo",0,138,1,1,1);

            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }

        window.cleanup();
        CleanupManager.exit(0);
        handler.cleanup();
    }

    private static void tesst() {
        logger.info("Pressed");
    }
}