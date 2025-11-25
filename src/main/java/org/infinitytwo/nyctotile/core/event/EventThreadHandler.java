package org.infinitytwo.nyctotile.core.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class EventThreadHandler {
    private static ExecutorService executorService = null;
    private static final int max = 20;
    private static final Map<Runnable, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            executorService = Executors.newFixedThreadPool(max, r -> new Thread(r, "EventWorker-" + System.identityHashCode(r)));
            initialized = true;
        }
    }

    public static void execute(Runnable task) {
        if (!initialized) {
            throw new IllegalStateException("EventThreadHandler has not been initialized. Call init() first.");
        }
        Future<?> future = executorService.submit(task);
        runningTasks.put(task, future);
    }

    public static boolean terminateTask(Runnable task) {
        Future<?> future = runningTasks.get(task);
        if (future != null) {
            return future.cancel(true); // Interrupt the thread if it's running
        }
        return false; // Task not found or not running
    }

    public static void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            initialized = false;
            executorService = null;
            runningTasks.clear();
        }
    }
}