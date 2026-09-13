package com.petchat.api.signalrclient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackoffRetry {
    private static final int[] DELAYS_MS = {1000, 2000, 5000, 10000, 30000};
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private int attempt = 0;

    public void schedule(Runnable action) {
        int delay = DELAYS_MS[Math.min(attempt, DELAYS_MS.length - 1)];
        attempt++;
        scheduler.schedule(action, delay, TimeUnit.MILLISECONDS);
    }

    public void reset() {
        attempt = 0;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}