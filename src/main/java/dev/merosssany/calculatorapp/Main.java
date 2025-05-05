package dev.merosssany.calculatorapp;

import dev.merosssany.calculatorapp.core.*;
import dev.merosssany.calculatorapp.core.event.MouseHoverEvent;
import dev.merosssany.calculatorapp.core.io.InputHandler;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.ui.Button;
import dev.merosssany.calculatorapp.core.ui.InteractableUI;
import dev.merosssany.calculatorapp.core.ui.UI;
import dev.merosssany.calculatorapp.logging.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static final Logger logger = new Logger("Main");

    public static void main(String[] args) {
         GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        logger.log("Launching game...");

        Window window = new Window(700,450,"Hello");
        window.initOpenGL();

        UI topLeftHalfElement = new UI("Test", new UIVector2Df(-1f,1f),2f,0.5f ,new RGBA(1.0f, 0.0f, 0.0f,0.9f));
        InteractableUI t = new InteractableUI(new UIVector2Df(-1f,0f),2f,0.5f ,new RGBA(0.0f, 1.0f, 0.0f,1f),window);
        InputHandler handler = new InputHandler(window);
        handler.init();
        Button button = new Button(new UIVector2Df(-1f,1f),2f,0.5f,new RGBA(0,0,0,1),window);

        while (!glfwWindowShouldClose(window.getWindow())) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer

            EventBus.post(new MouseHoverEvent());

//            topLeftHalfElement.draw();
//            t.draw();
            button.draw();


            long vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
//            int font = FontLoader.loadFont(vg, "oswald", "/fonts/Main.ttf");

            nvgBeginFrame(vg, window.getWidth(), window.getHeight(), 1);
            nvgFontFace(vg, "oswald");
            nvgFontSize(vg, 32f);
            nvgFillColor(vg, nvgRGBA((byte) 255, (byte) 255, (byte) 255, (byte) 255, NVGColor.create()));
            nvgText(vg, 100, 100, "Hello from NanoVG!");
            nvgEndFrame(vg);

            glfwSwapBuffers(window.getWindow()); // Swap the color buffers
            glfwPollEvents();       // Poll for window events
        }

        window.cleanup();
        CleanupManager.exit(0);
    }

    private static void tesst() {
        logger.log("Button Pressed");
    }
}