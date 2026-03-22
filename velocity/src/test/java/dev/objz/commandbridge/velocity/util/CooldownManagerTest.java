package dev.objz.commandbridge.velocity.util;

import dev.objz.commandbridge.logging.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownManagerTest {

    private static final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeAll
    static void installLog() {
        try {
            Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException e) {
            // Log already installed
        }
    }

    @Test
    void notOnCooldownWhenNotSet() {
        CooldownManager manager = new CooldownManager();
        assertFalse(manager.isOnCooldown("script", TEST_UUID));
    }

    @Test
    void onCooldownAfterSet() {
        CooldownManager manager = new CooldownManager();
        manager.setCooldown("script", TEST_UUID, Duration.ofSeconds(60));
        assertTrue(manager.isOnCooldown("script", TEST_UUID));
    }

    @Test
    void remainingPositiveWhenActive() {
        CooldownManager manager = new CooldownManager();
        manager.setCooldown("script", TEST_UUID, Duration.ofSeconds(60));
        Duration remaining = manager.getRemaining("script", TEST_UUID);
        assertTrue(remaining.compareTo(Duration.ZERO) > 0);
    }

    @Test
    void remainingZeroWhenNotSet() {
        CooldownManager manager = new CooldownManager();
        Duration remaining = manager.getRemaining("script", TEST_UUID);
        assertTrue(remaining.equals(Duration.ZERO));
    }

    @Test
    void zeroDurationIgnored() {
        CooldownManager manager = new CooldownManager();
        manager.setCooldown("script", TEST_UUID, Duration.ZERO);
        assertFalse(manager.isOnCooldown("script", TEST_UUID));
    }

    @Test
    void negativeDurationIgnored() {
        CooldownManager manager = new CooldownManager();
        manager.setCooldown("script", TEST_UUID, Duration.ofSeconds(-1));
        assertFalse(manager.isOnCooldown("script", TEST_UUID));
    }

    @Test
    void independentScriptCooldowns() {
        CooldownManager manager = new CooldownManager();
        manager.setCooldown("scriptA", TEST_UUID, Duration.ofSeconds(60));
        assertFalse(manager.isOnCooldown("scriptB", TEST_UUID));
    }
}
