package dev.objz.commandbridge.scripting.validation;

import dev.objz.commandbridge.TestFixtures;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ProblemSink}.
 * Verifies error accumulation, immutability, and formatted output.
 */
final class ProblemSinkTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void errorAddsProblemHasErrorsReturnsTrue() {
        ProblemSink sink = new ProblemSink();
        sink.error("script.yml", "missing field");
        assertTrue(sink.hasErrors());
        assertEquals(1, sink.count());
    }

    @Test
    void hasErrorsNoProblemAddedReturnsFalse() {
        ProblemSink sink = new ProblemSink();
        assertFalse(sink.hasErrors());
    }

    @Test
    void problemsReturnsImmutableList() {
        ProblemSink sink = new ProblemSink();
        sink.error("path", "msg");
        assertThrows(UnsupportedOperationException.class,
                () -> sink.problems().add(new ProblemSink.Problem("x", "y")));
    }

    @Test
    void toBulletedListFormatsBulletsCorrectly() {
        ProblemSink sink = new ProblemSink();
        sink.error("a.yml", "bad value");
        sink.error("b.yml", "missing key");
        String result = sink.toBulletedList("Errors:");
        assertTrue(result.startsWith("Errors:\n"));
        assertTrue(result.contains("  - a.yml: bad value\n"));
        assertTrue(result.contains("  - b.yml: missing key\n"));
    }

    @Test
    void toStringFormatsNewlineSeparated() {
        ProblemSink sink = new ProblemSink();
        sink.error("file.yml", "oops");
        String result = sink.toString();
        assertTrue(result.contains("file.yml: oops"));
    }
}
