package com.legalai.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
    private final ConcurrentHashMap<String, EvictableEntry> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;
    private static final long EVICT_TTL_MS = 3600_000;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            1,
            r -> {
                Thread t = new Thread(r, "rate-limiter-scheduler");
                t.setDaemon(true);
                return t;
            }
    );

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean tryAcquire(String key) {
        evictStale();
        EvictableEntry entry = windows.computeIfAbsent(key, k -> new EvictableEntry(new SlidingWindow(maxRequests, windowMs)));
        return entry.window.tryAcquire();
    }

    public void reset(String key) {
        windows.remove(key);
    }

    public void markRateLimited(String key) {
        windows.put(key, new EvictableEntry(new SlidingWindow(0, windowMs)));
        scheduler.schedule(() -> windows.remove(key), windowMs, TimeUnit.MILLISECONDS);
    }

    private void evictStale() {
        long now = System.currentTimeMillis();
        long cutoff = now - EVICT_TTL_MS;
        windows.entrySet().removeIf(e -> e.getValue().lastAccessTime < cutoff);
    }

    private static class EvictableEntry {
        final SlidingWindow window;
        volatile long lastAccessTime;

        EvictableEntry(SlidingWindow window) {
            this.window = window;
            this.lastAccessTime = System.currentTimeMillis();
        }
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
