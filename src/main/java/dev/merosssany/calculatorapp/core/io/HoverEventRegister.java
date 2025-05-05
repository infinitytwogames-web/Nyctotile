package dev.merosssany.calculatorapp.core.io;

import dev.merosssany.calculatorapp.core.AdvancedMath;
import dev.merosssany.calculatorapp.core.EventBus;
import dev.merosssany.calculatorapp.core.LocalEventBus;
import dev.merosssany.calculatorapp.core.Window;
import dev.merosssany.calculatorapp.core.event.*;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.core.ui.UI;
import dev.merosssany.calculatorapp.logging.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HoverEventRegister {
    private final Window window;
    private static final List<UI> listeners = Collections.synchronizedList(new ArrayList<>()); // Thread-safe
    private static final Logger logger = new Logger("Hover Event");
    private final Thread main;
    private static final LocalEventBus eventBus = new LocalEventBus("Hover Event");
    private float currentMouseX;
    private float currentMouseY;
    private volatile boolean running = true; // For graceful shutdown

    public static void registerUI(UI ui) {
        logger.log("Registering UI Element:", ui.getName());
        if (!listeners.contains(ui)) {
            listeners.add(ui);
        }
    }

    public HoverEventRegister(Window window) {
        this.window = window;
        main = new Thread(this::threadRunner);
        eventBus.register(this);
        main.start();
    }

    public void update() {
        double[] cursorPositionX = new double[1];
        double[] cursorPositionY = new double[1];
        GLFW.glfwGetCursorPos(window.getWindow(), cursorPositionX, cursorPositionY);
        double mouseX = cursorPositionX[0];
        double mouseY = cursorPositionY[0];
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        float normalizedMouseX = (float) ((2.0 * mouseX) / windowWidth - 1.0);
        float normalizedMouseY = (float) (1.0 - (2.0 * mouseY) / windowHeight);

        eventBus.post(new MouseCoordinatesEvent(normalizedMouseX, normalizedMouseY));
    }

    @SubscribeEvent
    public void onMouseCoordinates(MouseCoordinatesEvent event) {
        this.currentMouseX = event.getNormalizedX();
        this.currentMouseY = event.getNormalizedY();
    }

    public void shutdown() {
        running = false;
        main.interrupt(); // Signal the thread to stop
        try {
            main.join(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void threadRunner() {
        while (running) {
            Vector2D<Float> normalizedMousePos = new Vector2D<>(currentMouseX, currentMouseY);
            for (UI listener : listeners) {
                if (window != null && AdvancedMath.isVectorPointIncludedIn(
                        listener.getPosition(),
                        normalizedMousePos,
                        listener.getEnd()
                )) {
                    EventBus.post(new MouseHoverEvent(normalizedMousePos));
                }
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false; // Exit the loop on interruption
            }
        }
        logger.log("HoverEventRegister thread stopped.");
    }
}