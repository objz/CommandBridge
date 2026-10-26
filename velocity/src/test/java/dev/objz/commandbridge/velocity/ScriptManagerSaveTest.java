package dev.objz.commandbridge.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for ScriptManager save and delete used by the web editor. */
final class ScriptManagerSaveTest {

    @TempDir
    Path dataDir;

    private ScriptManager manager;
    private String example;

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setUp() throws Exception {
        manager = new ScriptManager(dataDir, null);
        try (InputStream in = getClass().getResourceAsStream("/example.yml")) {
            // PLAYERS needs PacketEvents on Velocity, which tests don't have
            example = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("type: PLAYERS", "type: TEXT");
        }
    }

    @Test
    void savesValidScriptVerbatimIncludingComments() throws Exception {
        assertEquals(java.util.List.of(), manager.save("example.yml", example));
        assertEquals(example, Files.readString(manager.scriptsDir().resolve("example.yml")));
    }

    @Test
    void rejectsPathTraversal() {
        assertFalse(manager.save("../evil.yml", example).isEmpty());
        assertFalse(manager.save("sub/evil.yml", example).isEmpty());
        assertFalse(Files.exists(dataDir.resolve("evil.yml")));
    }

    @Test
    void rejectsInvalidYamlWithoutWriting() {
        assertFalse(manager.save("broken.yml", "version: 4\nname: [").isEmpty());
        assertFalse(Files.exists(manager.scriptsDir().resolve("broken.yml")));
    }

    @Test
    void rejectsDuplicateScriptNameInAnotherFile() {
        assertTrue(manager.save("example.yml", example).isEmpty());
        manager.loadAll(false);
        assertFalse(manager.save("copy.yml", example).isEmpty());
        assertTrue(manager.save("example.yml", example).isEmpty());
    }

    @Test
    void deletesOnlyScriptFiles() throws Exception {
        manager.save("example.yml", example);
        assertFalse(manager.delete("../config.yml"));
        assertTrue(manager.delete("example.yml"));
        assertTrue(manager.readSources().isEmpty());
    }
}
