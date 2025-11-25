package org.infinitytwo.nyctotile.core.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Deprecated
public class AlgorithmTesting {

    private final Runtime runtime = Runtime.getRuntime();
    private final List<Long> memorySamples = Collections.synchronizedList(new ArrayList<>());

    private ScheduledExecutorService scheduler;
    private long startTime;
    private final long timeThresholdNanos;
    private final int sampleIntervalMillis;

    private long endTime;
    private boolean ended = false;

    public AlgorithmTesting(int sampleIntervalMillis, int timeThresholdSeconds) {
        this.sampleIntervalMillis = sampleIntervalMillis;
        this.timeThresholdNanos = TimeUnit.SECONDS.toNanos(timeThresholdSeconds);
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            System.out.println("AlgorithmTesting is already running.");
            return;
        }

        memorySamples.clear();
        ended = false;
        startTime = System.nanoTime();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            memorySamples.add(usedMemory);
        }, 0, sampleIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public int end() {
        if (ended) return getTotalScore(); // prevent re-ending

        endTime = System.nanoTime();
        ended = true;

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        return getTotalScore();
    }

    private int getTotalScore() {
        int timeScore = calculateTimeScore();
        int memoryScore = calculateMemoryScore();
        return (timeScore + memoryScore) / 2;
    }

    private int calculateTimeScore() {
        long duration = endTime - startTime;
        if (duration >= timeThresholdNanos)
            return 0;

        return Math.max(0, 100 - (int) ((duration * 100.0) / timeThresholdNanos));
    }

    private int calculateMemoryScore() {
        if (memorySamples.isEmpty()) return 100;

        double averageMemory = memorySamples.stream()
                .mapToLong(Long::longValue)
                .average().orElse(0);

        long maxMemory = runtime.maxMemory();
        double usageRatio = averageMemory / maxMemory;

        return Math.max(0, 100 - (int) (usageRatio * 100));
    }

    public void printReport(String modName) {
        long duration = endTime - startTime;
        double avgMem = memorySamples.stream().mapToLong(l -> l).average().orElse(0);
        long maxMem = runtime.maxMemory();

        System.out.println("----- Algorithm Testing Report: " + modName + " -----");
        System.out.printf("Execution Time: %.2f ms\n", duration / 1_000_000.0);
        System.out.printf("Average Memory: %.2f MB\n", avgMem / (1024.0 * 1024.0));
        System.out.printf("Max JVM Memory: %.2f MB\n", maxMem / (1024.0 * 1024.0));
        System.out.println("Performance Score: " + getTotalScore() + " / 100");
        System.out.println("---------------------------------------------------");
    }
}
