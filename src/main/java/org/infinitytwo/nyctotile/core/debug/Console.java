package org.infinitytwo.nyctotile.core.debug;

import javax.swing.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Deprecated
public class Console extends JFrame implements Runnable {
    private final JTextArea consoleOutputArea = new JTextArea();
    // This is the thread-safe queue for messages
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;

    // The constructor and UI setup remains the same...

    public void addMessage(String message) {
        messageQueue.add(message);
    }

    public void run() {
        while (running) {
            // Poll messages from the queue
            String message;
            while ((message = messageQueue.poll()) != null) {
                // Update the Swing UI on the EDT
                String finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    consoleOutputArea.append(finalMessage + "\n");
                    // Keep the scrollbar at the bottom
                    consoleOutputArea.setCaretPosition(consoleOutputArea.getDocument().getLength());
                });
            }
            // Add a small delay to prevent the thread from spinning too fast
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        running = false;
    }
}