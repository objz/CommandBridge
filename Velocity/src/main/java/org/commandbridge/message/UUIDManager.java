package org.commandbridge.message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UUIDManager {
    private final Map<String, Long> uuidMap;
    private final ScheduledExecutorService cleanupExecutor;
    private static final long UUID_EXPIRY_TIME_MS = 60000; // 1 minute expiry time for UUIDs

    public UUIDManager() {
        this.uuidMap = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        // Schedule cleanup task
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanUpExpiredUUIDs, UUID_EXPIRY_TIME_MS, UUID_EXPIRY_TIME_MS, TimeUnit.MILLISECONDS);
    }

    public boolean isUUIDProcessed(String uuid) {
        return uuidMap.containsKey(uuid);
    }

    public void addUUID(String uuid) {
        uuidMap.put(uuid, System.currentTimeMillis());
    }

    private void cleanUpExpiredUUIDs() {
        long now = System.currentTimeMillis();
        uuidMap.entrySet().removeIf(entry -> now - entry.getValue() > UUID_EXPIRY_TIME_MS);
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}
