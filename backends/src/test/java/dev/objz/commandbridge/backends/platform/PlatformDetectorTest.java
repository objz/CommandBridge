package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.scripting.model.enums.ServerPlatform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

/**
 * Tests for {@link PlatformDetector}.
 * Verifies that platform detection returns valid, consistent results.
 */
final class PlatformDetectorTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void detectReturnsNonNull() {
        ServerPlatform result = PlatformDetector.detectPlatform();
        assertNotNull(result, "detectPlatform() must never return null");
    }

    @Test
    void detectReturnsConsistentResult() {
        ServerPlatform first = PlatformDetector.detectPlatform();
        ServerPlatform second = PlatformDetector.detectPlatform();
        assertEquals(first, second, "consecutive calls must return the same platform");
    }

    @Test
    void detectResultIsValidPlatform() {
        ServerPlatform result = PlatformDetector.detectPlatform();
        Set<ServerPlatform> valid = Set.of(ServerPlatform.values());
        assertTrue(valid.contains(result),
                "result must be one of the defined Platform enum values");
    }
}
