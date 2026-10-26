package dev.objz.commandbridge.scripting.migration;

import dev.objz.commandbridge.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlMigratorTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static long count(String text, String needle) {
        return text.lines().filter(l -> l.trim().startsWith(needle)).count();
    }

    @Test
    void aKeyThatIsAlreadyThereIsNotAddedTwice() {
        String v3 = """
                version: 3
                name: test
                defaults:
                  server:
                    schedule-online: false
                    player-arg: "player"
                commands:
                  - command: say hi
                    server:
                      schedule-online: false
                      player-arg: "player"
                """;

        var result = new YamlMigrator(4).migrate(v3);

        String out = result.yaml();
        assertEquals(2, count(out, "player-arg:"), out);
        assertTrue(out.contains("player-arg: \"player\""), out);
        assertDoesNotThrow(() -> new Load(LoadSettings.builder().build()).loadFromString(out), out);
    }

    @Test
    void windowsLineEndingsStayWindowsLineEndings() {
        String v2 = String.join("\r\n",
                "version: 2",
                "name: test",
                "defaults:",
                "  server:",
                "    timeout: 5",
                "    schedule-online: false",
                "commands:",
                "  - command: say hi",
                "    server:",
                "      frequency: 2",
                "      schedule-online: false",
                "");

        String out = new YamlMigrator(4).migrate(v2).yaml();

        assertFalse(out.contains("timeout"), out);
        assertFalse(out.contains("frequency"), out);
        assertEquals(2, count(out, "player-arg:"), out);
        assertFalse(out.replace("\r\n", "").contains("\n"), "a bare \\n crept in: " + out.replace("\r", "\\r"));
        assertFalse(out.replace("\r\n", "").contains("\r"), "a stray \\r: " + out.replace("\r", "\\r"));
    }
}
