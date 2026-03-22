package dev.objz.commandbridge.scripting.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import dev.objz.commandbridge.logging.Log;

class PlaceholderExtractorTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(LoggerFactory.getLogger("test"));
        } catch (IllegalStateException e) {
            // Log already installed, ignore
        }
    }

    @Test
    void testSinglePlaceholder() {
        List<String> result = PlaceholderExtractor.extract("/give ${player} diamond");
        assertEquals(List.of("player"), result);
    }

    @Test
    void testMultiplePlaceholders() {
        List<String> result = PlaceholderExtractor.extract("${cmd} ${target}");
        assertEquals(List.of("cmd", "target"), result);
    }

    @Test
    void testNoPlaceholders() {
        List<String> result = PlaceholderExtractor.extract("plain command");
        assertTrue(result.isEmpty());
    }

    @Test
    void testEmptyString() {
        List<String> result = PlaceholderExtractor.extract("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testNullString() {
        List<String> result = PlaceholderExtractor.extract(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBlankString() {
        List<String> result = PlaceholderExtractor.extract("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    void testRepeatedPlaceholder() {
        List<String> result = PlaceholderExtractor.extract("${a} ${a}");
        assertEquals(List.of("a", "a"), result);
    }

    @Test
    void testAdjacentPlaceholders() {
        List<String> result = PlaceholderExtractor.extract("${x}${y}");
        assertEquals(List.of("x", "y"), result);
    }

    @Test
    void testPlaceholderWithWhitespace() {
        List<String> result = PlaceholderExtractor.extract("${ player }");
        assertEquals(List.of("player"), result);
    }

    @Test
    void testBlankPlaceholder() {
        List<String> result = PlaceholderExtractor.extract("${} command");
        assertTrue(result.isEmpty());
    }

    @Test
    void testPlaceholderWithSpaces() {
        List<String> result = PlaceholderExtractor.extract("${  } command");
        assertTrue(result.isEmpty());
    }

    @Test
    void testMixedValidAndBlank() {
        List<String> result = PlaceholderExtractor.extract("${valid} ${} ${another}");
        assertEquals(List.of("valid", "another"), result);
    }

    @Test
    void testExtractAllWithMultipleCommands() {
        List<String> commands = List.of(
            "/give ${player} diamond",
            "${cmd} ${target}"
        );
        List<String> result = PlaceholderExtractor.extractAll(commands);
        assertEquals(List.of("player", "cmd", "target"), result);
    }

    @Test
    void testExtractAllWithEmptyList() {
        List<String> result = PlaceholderExtractor.extractAll(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractAllWithNullList() {
        List<String> result = PlaceholderExtractor.extractAll(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractAllWithNullCommand() {
        List<String> commands = new java.util.ArrayList<>();
        commands.add("/give ${player} diamond");
        commands.add(null);
        commands.add("${cmd} ${target}");
        List<String> result = PlaceholderExtractor.extractAll(commands);
        assertEquals(List.of("player", "cmd", "target"), result);
    }

    @Test
    void testExtractAllWithBlankCommand() {
        List<String> commands = List.of(
            "/give ${player} diamond",
            "   ",
            "${cmd} ${target}"
        );
        List<String> result = PlaceholderExtractor.extractAll(commands);
        assertEquals(List.of("player", "cmd", "target"), result);
    }

    @Test
    void testExtractAllWithDuplicates() {
        List<String> commands = List.of(
            "${a} ${a}",
            "${a} ${b}"
        );
        List<String> result = PlaceholderExtractor.extractAll(commands);
        assertEquals(List.of("a", "a", "a", "b"), result);
    }

    @Test
    void testComplexCommand() {
        List<String> result = PlaceholderExtractor.extract(
            "execute as ${player} at @s run give @s ${item} ${amount}"
        );
        assertEquals(List.of("player", "item", "amount"), result);
    }

    @Test
    void testPlaceholderWithSpecialChars() {
        List<String> result = PlaceholderExtractor.extract("${player_name}");
        assertEquals(List.of("player_name"), result);
    }

    @Test
    void testPlaceholderWithNumbers() {
        List<String> result = PlaceholderExtractor.extract("${arg1} ${arg2}");
        assertEquals(List.of("arg1", "arg2"), result);
    }

}
