package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.backends.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link PathsUtil}.
 * Verifies data directory normalization handles casing and edge cases.
 */
final class PathsUtilTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void normalizeDataDirMixedCaseReturnsLowercase() {
        Path input = Path.of("plugins", "CommandBridge");
        Path result = PathsUtil.normalizeDataDir(input);
        assertEquals("commandbridge", result.getFileName().toString(),
                "filename component must be lowercased");
        assertEquals(Path.of("plugins"), result.getParent(),
                "parent directory must be preserved");
    }

    @Test
    void normalizeDataDirAlreadyLowercaseUnchanged() {
        Path input = Path.of("plugins", "commandbridge");
        Path result = PathsUtil.normalizeDataDir(input);
        assertEquals(input, result,
                "already-lowercase path must remain unchanged");
    }

    @Test
    void normalizeDataDirNullReturnsNull() {
        assertNull(PathsUtil.normalizeDataDir(null),
                "null input must return null");
    }

    @Test
    void normalizeDataDirNoParentReturnsUnchanged() {
        Path input = Path.of("commandbridge");
        Path result = PathsUtil.normalizeDataDir(input);
        assertEquals(input, result,
                "path with no parent must be returned as-is");
    }
}
