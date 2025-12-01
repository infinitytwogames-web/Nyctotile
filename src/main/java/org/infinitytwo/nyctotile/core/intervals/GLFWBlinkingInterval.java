package org.infinitytwo.nyctotile.core.intervals; // Or wherever you want to put utility classes

import org.infinitytwo.nyctotile.core.event.Event;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.manager.WorkerThreads;

@Deprecated
public class GLFWBlinkingInterval {
    // volatile ensures changes to this flag are immediately visible across threads
    private volatile boolean isRunning = false;
    private Thread eventThread; // Correctly not final, as it can be reassigned
    private final Runnable action;
    private final long activeDurationNanos;   // Time for the "on" phase in nanoseconds
    private final long inactiveDurationNanos; // Time for the "off" phase in nanoseconds
    private volatile boolean isCurrentlyActive; // Current state of the blink: true for "on", false for "off"
    private final EventBus eventBus;

    /**
     * Creates a new blinking/pulsing interval timer that runs on its own background thread.
     * The associated action is executed each time the active phase begins.
     *
     * @param action The {@code Runnable} to execute when the active phase begins.
     * WARNING: This action will run on a background thread. If it touches UI,
     * it MUST be dispatched back to the main thread (which is now handled internally).
     * @param activeMillis The duration (in milliseconds) for which the interval is "active".
     * @param inactiveMillis The duration (in milliseconds) for which the interval is "inactive".
     */
    public GLFWBlinkingInterval(Runnable action, long activeMillis, long inactiveMillis) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null.");
        }
        // Ensure durations are non-negative by taking absolute value if negative.
        // Or, you could throw IllegalArgumentException here for strictly positive input.
        if (activeMillis < 0) {
            activeMillis = -activeMillis;
        }
        if (inactiveMillis < 0) {
            inactiveMillis = -inactiveMillis;
        }

        this.action = action;
        this.activeDurationNanos = activeMillis * 1_000_000L;
        this.inactiveDurationNanos = inactiveMillis * 1_000_000L;
        this.isCurrentlyActive = true; // Start in the active phase

        this.eventBus = new EventBus(); // Changed bus name for consistency
        eventBus.register(this); // Register this instance to handle its own events

        // Initialize the thread but don't start it yet
        this.eventThread = new Thread(this::threadCode, "GLFWBlinkingInterval-Thread"); // Changed thread name
    }

    private void threadCode() {
        long lastPhaseChangeTime = System.nanoTime(); // Use real time for accurate timing

        // Post initial event if starting in active phase
        if (isCurrentlyActive) {
            eventBus.post(new IntervalRun());
        }

        while (isRunning) { // Loop will continue as long as isRunning is true
            long currentTime = System.nanoTime();
            long elapsedInCurrentPhase = currentTime - lastPhaseChangeTime;

            long phaseDuration = isCurrentlyActive ? activeDurationNanos : inactiveDurationNanos;

            if (elapsedInCurrentPhase >= phaseDuration) {
                // Time to switch phase
                isCurrentlyActive = !isCurrentlyActive; // Toggle the state
                lastPhaseChangeTime = currentTime;      // Reset the timer for the new phase

                if (isCurrentlyActive) {
                    // Only post event when the interval *becomes* active
                    eventBus.post(new IntervalRun());
                }
            }

            // Calculate remaining time for the current phase to avoid busy-waiting
            long timeUntilNextChange = phaseDuration - (System.nanoTime() - lastPhaseChangeTime);
            long sleepMillis = timeUntilNextChange / 1_000_000L; // Convert nanoseconds to milliseconds

            // Only sleep if there's a significant time left to avoid very small sleeps or negative sleep times.
            if (sleepMillis > 0) {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    // Thread was interrupted (e.g., by end() call)
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    isRunning = false; // Set flag to false to exit the loop gracefully
                }
            } else {
                // If sleepMillis is 0 or negative, it means the phase change is due or past due.
                // Yield control to other threads to avoid tight loop, especially if a very short duration.
                Thread.yield();
            }
        }
    }

    /**
     * Starts the blinking cycle. If the thread hasn't been started yet, it begins execution.
     * If the thread has terminated, it recreates and starts it.
     */
    public void start() {
        if (!isRunning) { // Only attempt to start if not already running
            isRunning = true; // Set flag to true

            // Ensure the thread is in a state where it can be started
            if (eventThread.getState() == Thread.State.NEW) {
                eventThread.start();
            } else if (eventThread.getState() == Thread.State.TERMINATED) {
                // If the thread terminated previously, create a new one to restart
                this.eventThread = new Thread(this::threadCode, "GLFWBlinkingInterval-Thread"); // Changed thread name
                eventThread.start();
            }
            // If it's already running (RUNNABLE, TIMED_WAITING, etc.), do nothing.
            // Consider calling reset() here if 'start()' should always reset the timer.
        }
    }

    /**
     * Stops the blinking cycle. Sets the internal flag to false and interrupts the thread
     * to wake it from sleep, allowing it to terminate gracefully.
     */
    public void end() {
        if (isRunning) { // Only attempt to stop if currently running
            isRunning = false; // Signal the thread to stop
            eventThread.interrupt(); // Interrupt to break out of sleep/yield
            try {
                // Wait briefly for the thread to actually die, making shutdown more robust.
                eventThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status if interrupted during join
            }
        }
    }

    /**
     * Resets the blinking cycle to its initial active state and restarts its internal timer.
     * If the interval was running, it will be stopped and then restarted.
     */
    public void reset() {
        boolean wasRunning = isRunning;
        if (wasRunning) {
            end(); // Stop it gracefully first
        }
        this.isCurrentlyActive = true; // Reset state to active
        // The lastPhaseChangeTime will be reset inside threadCode() when start() is called
        if (wasRunning) {
            start(); // Start again to re-initialize time and loop
        }
    }

    /**
     * Returns the current state of the blinking interval (active or inactive).
     * This method is thread-safe as `isCurrentlyActive` is volatile.
     * @return true if the interval is currently in its active phase, false otherwise.
     */
    public boolean isCurrentlyActive() {
        return isCurrentlyActive;
    }

    /**
     * Event handler that gets called when the IntervalRun event is posted.
     * This method runs on the GLFWBlinkingInterval's dedicated background thread.
     * The `action` is immediately dispatched to the main application thread for safe UI updates.
     * @param e The IntervalRun event.
     */
    @SubscribeEvent
    private void eventHandler(IntervalRun e) {
        // Dispatch the user-provided action to the main application thread for safe execution.
        WorkerThreads.dispatch(action);
    }

    // Event class for internal use
    private static class IntervalRun extends Event {}
}