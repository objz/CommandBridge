package dev.objz.commandbridge.velocity.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CooldownManager} — verifies cooldown setting, checking,
 * remaining duration, zero-duration handling, and per-player/per-script isolation.
 */
final class CooldownManagerTest {

    private CooldownManager manager;

    @BeforeEach
    void setUp() {
        manager = new CooldownManager();
    }

    @Test
    void isOnCooldownAfterSetReturnsTrue() {
        UUID uuid = UUID.randomUUID();
        manager.setCooldown("test-script", uuid, Duration.ofMinutes(5));

        assertTrue(manager.isOnCooldown("test-script", uuid));
    }

    @Test
    void isOnCooldownNoSetReturnsFalse() {
        UUID uuid = UUID.randomUUID();

        assertFalse(manager.isOnCooldown("test-script", uuid));
    }

    @Test
    void getRemainingAfterSetReturnsPositive() {
        UUID uuid = UUID.randomUUID();
        manager.setCooldown("test-script", uuid, Duration.ofMinutes(5));

        Duration remaining = manager.getRemaining("test-script", uuid);
        assertTrue(remaining.toMillis() > 0,
                "Remaining duration should be positive immediately after setting cooldown");
    }

    @Test
    void setCooldownZeroDurationNotOnCooldown() {
        UUID uuid = UUID.randomUUID();
        manager.setCooldown("test-script", uuid, Duration.ZERO);

        assertFalse(manager.isOnCooldown("test-script", uuid));
    }

    @Test
    void setCooldownDifferentPlayersAreIndependent() {
        UUID playerA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID playerB = UUID.fromString("00000000-0000-0000-0000-000000000002");

        manager.setCooldown("test-script", playerA, Duration.ofMinutes(5));

        assertTrue(manager.isOnCooldown("test-script", playerA));
        assertFalse(manager.isOnCooldown("test-script", playerB));
    }

    @Test
    void setCooldownDifferentScriptsAreIndependent() {
        UUID uuid = UUID.randomUUID();

        manager.setCooldown("script-x", uuid, Duration.ofMinutes(5));

        assertTrue(manager.isOnCooldown("script-x", uuid));
        assertFalse(manager.isOnCooldown("script-y", uuid));
    }
}
