package dev.objz.commandbridge.util;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class RateLimiter<K> {
    private static final class Window {
        final AtomicLong sec = new AtomicLong();
        final AtomicInteger count = new AtomicInteger();
    }

    private final int maxPerSec;
    private final ConcurrentHashMap<K, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxPerSec) {
        if (maxPerSec <= 0)
            throw new IllegalArgumentException("maxPerSec must be > 0");
        this.maxPerSec = maxPerSec;
    }

    public boolean allow(K key) {
        long now = Instant.now().getEpochSecond();
        Window w = windows.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (w.sec.get() != now) {
                w.sec.set(now);
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= maxPerSec;
        }
    }
}
