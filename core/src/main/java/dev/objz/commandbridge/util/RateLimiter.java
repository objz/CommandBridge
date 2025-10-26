package dev.objz.commandbridge.util;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter<K> {
	private static final class Window {
		volatile long sec;
		volatile int count;
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
		if (w.sec != now) {
			w.sec = now;
			w.count = 0;
		}
		return ++w.count <= maxPerSec;
	}
}
