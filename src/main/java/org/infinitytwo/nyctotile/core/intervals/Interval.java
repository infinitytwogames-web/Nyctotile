package org.infinitytwo.nyctotile.core.intervals; // Or wherever you want to put utility classes

public class Interval {
    private volatile long lastActionTime; // Stores System.nanoTime() when the action last occurred
    private volatile long intervalDurationNanos; // Desired interval in nanoseconds
    private final Runnable action;
    private boolean run; // The code to execute when the interval passes

    /**
     * Creates a new non-blocking, one-threaded interval timer.
     * This timer must be updated by calling its `update()` method regularly
     * from your main application loop. The action will be executed on the same thread
     * that calls `update()`.
     *
     * @param intervalMillis The desired interval duration in milliseconds (e.g., 1000 for 1 second).
     * @param action The {@code Runnable} to execute when the interval passes. Cannot be null.
     * @throws IllegalArgumentException if the {@code action} is null.
     */
    public Interval(long intervalMillis, Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("Runnable action cannot be null.");
        }
        this.intervalDurationNanos = intervalMillis * 1_000_000L; // Convert milliseconds to nanoseconds
        this.action = action;
        this.lastActionTime = System.nanoTime(); // Initialize the timer to the current time
        run = false;
    }

    /**
     * Call this method from your main application loop (e.g., game loop or render loop).
     * It checks if enough time has passed since the last execution of the action.
     * If the interval has elapsed, the associated {@code Runnable} action is executed,
     * and the timer is reset for the next interval.
     */
    public void update() {
        if (!run) return;
        long currentTime = System.nanoTime();
        if (currentTime - lastActionTime >= intervalDurationNanos) {
            action.run(); // Execute the custom action
            lastActionTime = currentTime; // Reset the timer for the next interval
        }
    }

    /**
     * Resets the timer, causing the next interval to start counting from the moment this method is called.
     * The action will be executed after a full interval duration from the reset time.
     */
    public void reset() {
        this.lastActionTime = System.nanoTime();
    }

    /**
     * Dynamically changes the interval duration for this timer.
     *
     * @param newIntervalMillis The new desired interval duration in milliseconds.
     */
    public void setIntervalMillis(long newIntervalMillis) {
        this.intervalDurationNanos = newIntervalMillis * 1_000_000L;
    }

    /**
     * Gets the current interval duration in milliseconds.
     * @return The interval duration in milliseconds.
     */
    public long getIntervalMillis() {
        return intervalDurationNanos / 1_000_000L;
    }

    public void end() {
        run = false;
    }

    public void start() {
        run = true;
    }
}