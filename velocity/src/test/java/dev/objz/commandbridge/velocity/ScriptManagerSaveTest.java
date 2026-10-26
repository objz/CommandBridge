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

    @Test
    void malformedScriptsDoNotCrashReload() throws Exception {
        Files.createDirectories(manager.scriptsDir());
        Files.writeString(manager.scriptsDir().resolve("broken.yml"), "version: 4\nname: [");

        manager.loadAll(false);

        assertTrue(manager.errors() > 0);
        assertTrue(manager.enabled().isEmpty());
    }

    @Test
    void rejectsDuplicateNamesWithoutAnInterveningReload() {
        assertTrue(manager.save("example.yml", example).isEmpty());
        assertFalse(manager.save("copy.yml", example).isEmpty());
    }

    @Test
    void scriptNameLookupIgnoresCase() {
        assertTrue(manager.save("example.yml", example).isEmpty());
        manager.loadAll(false);
        String name = manager.loaded().getFirst().name();

        assertTrue(manager.filePathOf(name.toUpperCase(java.util.Locale.ROOT)).isPresent());
    }

    @Test
    void doesNotReadScriptsThroughSymlinks() throws Exception {
        Files.createDirectories(manager.scriptsDir());
        Path outside = dataDir.resolve("private.yml");
        Files.writeString(outside, example);
        Files.createSymbolicLink(manager.scriptsDir().resolve("linked.yml"), outside);

        assertTrue(manager.readSources().isEmpty());
        manager.loadAll(false);
        assertTrue(manager.loaded().isEmpty());
    }

    @Test
    void savingDoesNotFollowAPredictableTemporarySymlink() throws Exception {
        Files.createDirectories(manager.scriptsDir());
        Path outside = dataDir.resolve("private.txt");
        Files.writeString(outside, "keep this");
        Files.createSymbolicLink(manager.scriptsDir().resolve("example.yml.tmp"), outside);

        assertTrue(manager.save("example.yml", example).isEmpty());
        assertEquals("keep this", Files.readString(outside));
    }

    @Test
    void enablingAnImplicitlyEnabledScriptKeepsTheVersionLineValid() throws Exception {
        assertTrue(manager.save("example.yml", example.replaceFirst("(?m)^enabled:.*\\R", "")).isEmpty());
        manager.loadAll(false);
        String name = manager.loaded().getFirst().name();

        assertEquals(ScriptManager.ToggleStatus.CHANGED, manager.setEnabled(name, false).status());

        assertEquals(0, manager.errors());
        assertEquals(1, manager.disabled().size());
        assertTrue(Files.readString(manager.scriptsDir().resolve("example.yml")).contains("version: 4\n"));
    }
}
