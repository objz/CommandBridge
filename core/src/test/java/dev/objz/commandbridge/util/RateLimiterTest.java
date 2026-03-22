package dev.objz.commandbridge.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @BeforeAll
    static void installLog() {
        try {
            dev.objz.commandbridge.logging.Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
            // expected
        }
    }

    @Test
    void constructorRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter<>(0));
    }

    @Test
    void constructorRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter<>(-1));
    }

    @Test
    void allowsUpToMax() {
        RateLimiter<String> limiter = new RateLimiter<>(5);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allow("k"), "Call " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void deniesOverMax() {
        RateLimiter<String> limiter = new RateLimiter<>(5);
        for (int i = 0; i < 5; i++) {
            limiter.allow("k");
        }
        assertFalse(limiter.allow("k"), "6th call should be denied");
    }

    @Test
    void independentKeys() {
        RateLimiter<String> limiter = new RateLimiter<>(2);
        assertTrue(limiter.allow("A"));
        assertTrue(limiter.allow("A"));
        assertFalse(limiter.allow("A"));
        assertTrue(limiter.allow("B"), "Key B should still be allowed");
        assertTrue(limiter.allow("B"));
        assertFalse(limiter.allow("B"));
    }

    @Test
    void windowResets() throws InterruptedException {
        RateLimiter<String> limiter = new RateLimiter<>(1);
        assertTrue(limiter.allow("k"), "First call should be allowed");
        assertFalse(limiter.allow("k"), "Second call in same second should be denied");
        Thread.sleep(1100);
        assertTrue(limiter.allow("k"), "Call after window reset should be allowed");
    }
}
