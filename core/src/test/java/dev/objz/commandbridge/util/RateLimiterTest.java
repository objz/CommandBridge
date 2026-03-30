package dev.objz.commandbridge.util;

import dev.objz.commandbridge.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RateLimiter}.
 * Verifies per-key rate limiting, window exhaustion, and invalid constructor arguments.
 */
final class RateLimiterTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void allowUnderLimitReturnsTrue() {
        RateLimiter<String> limiter = new RateLimiter<>(5);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allow("user"), "Call " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void allowAtLimitReturnsFalse() {
        RateLimiter<String> limiter = new RateLimiter<>(3);
        assertTrue(limiter.allow("key"));
        assertTrue(limiter.allow("key"));
        assertTrue(limiter.allow("key"));
        assertFalse(limiter.allow("key"), "Fourth call should exceed limit of 3");
    }

    @Test
    void allowDifferentKeysAreIndependent() {
        RateLimiter<String> limiter = new RateLimiter<>(1);
        assertTrue(limiter.allow("a"), "First call for key 'a' should be allowed");
        assertFalse(limiter.allow("a"), "Second call for key 'a' should be blocked");
        assertTrue(limiter.allow("b"), "First call for key 'b' should still be allowed");
    }

    @Test
    void allowExhaustedKeyBlocksFurtherCalls() {
        RateLimiter<String> limiter = new RateLimiter<>(2);
        assertTrue(limiter.allow("x"));
        assertTrue(limiter.allow("x"));
        assertFalse(limiter.allow("x"), "Third call should be blocked");
        assertFalse(limiter.allow("x"), "Fourth call should also be blocked");
        assertFalse(limiter.allow("x"), "Fifth call should also be blocked");
    }

    @Test
    void constructorZeroLimitThrows() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter<>(0));
    }

    @Test
    void constructorNegativeLimitThrows() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter<>(-5));
    }
}
