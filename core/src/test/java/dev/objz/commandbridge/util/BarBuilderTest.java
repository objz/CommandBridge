package dev.objz.commandbridge.util;

import dev.objz.commandbridge.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BarBuilder}.
 * Verifies proportional fill distribution, empty-bar format, and custom fill characters.
 */
final class BarBuilderTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static String stripTags(String input) {
        return input.replaceAll("<[^>]+>", "");
    }

    @Test
    void buildSingleSegmentFillsEntireWidth() {
        String result = BarBuilder.create(10).add("green", 1.0).build();
        String stripped = stripTags(result);
        assertEquals("[||||||||||]", stripped);
    }

    @Test
    void buildEqualSegmentsEvenDistribution() {
        String result = BarBuilder.create(10).add("green", 1.0).add("red", 1.0).build();
        assertTrue(result.contains("<green>" + "|".repeat(5) + "</green>"));
        assertTrue(result.contains("<red>" + "|".repeat(5) + "</red>"));
    }

    @Test
    void buildNoSegmentsReturnsBracketedSpaces() {
        String result = BarBuilder.create(5).build();
        assertFalse(result.isEmpty());
        String stripped = stripTags(result);
        assertEquals("[     ]", stripped);
    }

    @Test
    void buildZeroWeightSegmentGetsNoWidth() {
        String result = BarBuilder.create(10).add("green", 1.0).add("red", 0.0).build();
        assertFalse(result.contains("<red>"));
        assertTrue(result.contains("<green>" + "|".repeat(10) + "</green>"));
    }

    @Test
    void buildTotalWidthIsAlwaysExact() {
        String result = BarBuilder.create(10)
                .add("green", 1.0).add("red", 1.0).add("blue", 1.0).build();
        String stripped = stripTags(result);
        String inner = stripped.substring(1, stripped.length() - 1);
        assertEquals(10, inner.length());
    }

    @Test
    void buildCustomFillCharUsed() {
        String result = BarBuilder.create(10).fill("=").add("green", 1.0).build();
        String stripped = stripTags(result);
        assertEquals("[==========]", stripped);
        assertFalse(stripped.contains("|"));
    }
}
