package dev.objz.commandbridge.scripting.bind;

import dev.objz.commandbridge.TestFixtures;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PlaceholderExtractor}.
 * Verifies ${key} placeholder extraction from command strings, including duplicate preservation.
 */
final class PlaceholderExtractorTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void extractSinglePlaceholderReturnsName() {
        List<String> result = PlaceholderExtractor.extract("say ${player}");
        assertEquals(List.of("player"), result);
    }

    @Test
    void extractMultiplePlaceholdersReturnsAll() {
        List<String> result = PlaceholderExtractor.extract("tp ${from} ${to}");
        assertEquals(List.of("from", "to"), result);
    }

    @Test
    void extractNoPlaceholdersReturnsEmpty() {
        List<String> result = PlaceholderExtractor.extract("say hello");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractNullInputReturnsEmpty() {
        List<String> result = PlaceholderExtractor.extract(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractEmptyInputReturnsEmpty() {
        List<String> result = PlaceholderExtractor.extract("");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractDuplicatePlaceholdersPreservesDuplicates() {
        List<String> result = PlaceholderExtractor.extract("${x} ${x}");
        assertEquals(List.of("x", "x"), result);
    }

    @Test
    void extractAdjacentPlaceholdersReturnsBoth() {
        List<String> result = PlaceholderExtractor.extract("${a}${b}");
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void extractAllMultipleCommandsReturnsCombined() {
        List<String> commands = List.of("say ${player}", "tp ${from} ${to}", "msg ${player}");
        List<String> result = PlaceholderExtractor.extractAll(commands);
        assertEquals(List.of("player", "from", "to", "player"), result);
    }

    @Test
    void extractReturnedListIsImmutable() {
        List<String> result = PlaceholderExtractor.extract("say ${player}");
        assertThrows(UnsupportedOperationException.class, () -> result.add("extra"));
    }
}
