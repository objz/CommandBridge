package dev.objz.commandbridge.velocity.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CooldownManager {

    private final Cache<String, Instant> cooldowns;

    public CooldownManager() {
        this.cooldowns = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();
    }

    public boolean isOnCooldown(String scriptName, UUID uuid) {
        Instant end = cooldowns.getIfPresent(key(scriptName, uuid));
        return end != null && Instant.now().isBefore(end);
    }

    public Duration getRemaining(String scriptName, UUID uuid) {
        Instant end = cooldowns.getIfPresent(key(scriptName, uuid));
        if (end == null)
            return Duration.ZERO;
        return Duration.between(Instant.now(), end);
    }

    public void setCooldown(String scriptName, UUID uuid, Duration duration) {
        if (duration.isZero() || duration.isNegative())
            return;
        cooldowns.put(key(scriptName, uuid), Instant.now().plus(duration));
    }

    private String key(String scriptName, UUID uuid) {
        return scriptName + ":" + uuid.toString();
    }
}
