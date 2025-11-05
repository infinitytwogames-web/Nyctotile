package org.infinitytwo.umbralore.core.manager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ChunkManager {
    private static final WorkerThreads threads = new WorkerThreads("chunk",5);
    
    public static int getRunningThreads() {
        return WorkerThreads.getRunningThreads();
    }
    
    public static void dispatch(Runnable task) {
        WorkerThreads.dispatch(task);
    }
    
    public static void run() {
        WorkerThreads.run();
    }
    
    public static Future<?> run(Runnable f) {
        return threads.run(f);
    }
    
    public static <T> Future<T> run(Callable<T> f) {
        return threads.run(f);
    }
    
    public static <T> Future<T> run(Runnable f, T result) {
        return threads.run(f, result);
    }
    
    public static void shutdown() {
        threads.shutdown();
    }
    
    public static boolean isTerminated() {
        return threads.isTerminated();
    }
    
    public static boolean isShutdown() {
        return threads.isShutdown();
    }
    
    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return threads.invokeAll(tasks);
    }
    
    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return threads.invokeAll(tasks, timeout, unit);
    }
    
    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return threads.invokeAny(tasks);
    }
    
    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return threads.invokeAny(tasks, timeout, unit);
    }
}
