package com.legalai.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

public class RateLimiter {
    private final Map<String, SlidingWindow> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean tryAcquire(String key) {
        SlidingWindow window = windows.computeIfAbsent(key, k -> new SlidingWindow(maxRequests, windowMs));
        return window.tryAcquire();
    }

    public void reset(String key) {
        windows.remove(key);
    }

    public void markRateLimited(String key) {
        windows.put(key, new SlidingWindow(0, windowMs));
        new Thread(() -> {
            try {
                Thread.sleep(windowMs);
                windows.remove(key);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private static class SlidingWindow {
        private final int maxRequests;
        private final long windowMs;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        SlidingWindow(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
        }

        boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMs) {
                synchronized (this) {
                    if (now - windowStart >= windowMs) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
