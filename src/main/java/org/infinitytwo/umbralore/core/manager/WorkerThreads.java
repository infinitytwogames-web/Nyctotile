package org.infinitytwo.umbralore.core.manager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerThreads {
    protected static final ThreadGroup group = new ThreadGroup("Worker Threads");
    private final ExecutorService service;
    protected static final AtomicInteger count = new AtomicInteger(-1);
    private static final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public static int getRunningThreads() {
        return count.get();
    }

    public static void dispatch(Runnable task) {
        tasks.add(task);
    }

    public static void run() {
        Runnable task = tasks.poll();
        if (task != null) task.run();
    }

    public WorkerThreads(int max) {
        service = Executors.newFixedThreadPool(max, new Factory("worker-thread"));
    }

    protected WorkerThreads(String name, int max) {
        service = Executors.newFixedThreadPool(max, new Factory(name+"-worker-thread"));
    }

    public Future<?> run(Runnable f) {
        return service.submit(f);
    }

    public <T> Future<T> run(Callable<T> f) {
        return service.submit(f);
    }

    public <T> Future<T> run(Runnable f, T result) {
        return service.submit(f,result);
    }

    public void shutdown() {
        service.shutdown();
    }

    public boolean isTerminated() {
        return service.isTerminated();
    }

    public boolean isShutdown() {
        return service.isShutdown();
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return service.invokeAll(tasks);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return service.invokeAll(tasks, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return service.invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return service.invokeAny(tasks, timeout, unit);
    }
}

class Factory implements ThreadFactory {
    private final String namePrefix;

    Factory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(WorkerThreads.group,r);

        t.setName(namePrefix + "-" + WorkerThreads.count.incrementAndGet());
        t.setDaemon(true); // Set the thread as a background/daemon thread

        return t;
    }
}
