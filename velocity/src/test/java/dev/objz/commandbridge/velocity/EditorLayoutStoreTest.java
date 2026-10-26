package dev.objz.commandbridge.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.velocity.editor.EditorLayoutStore;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for the editor layout file. */
final class EditorLayoutStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path dataDir;

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void missingFileReadsAsEmptyObject() {
        assertEquals(MAPPER.createObjectNode(), new EditorLayoutStore(dataDir).read());
    }

    @Test
    void writtenLayoutReadsBack() throws Exception {
        EditorLayoutStore store = new EditorLayoutStore(dataDir);
        ObjectNode layout = (ObjectNode) MAPPER.readTree("{\"nodes\":{\"hello.yml#cmd\":{\"x\":1,\"y\":2}},\"groups\":[]}");
        assertNull(store.write(layout));
        assertEquals(layout, store.read());
        assertTrue(Files.exists(dataDir.resolve("data/editor-layout.json")));
    }

    @Test
    void rejectsNonObjectsAndHugeLayouts() throws Exception {
        EditorLayoutStore store = new EditorLayoutStore(dataDir);
        assertNotNull(store.write(MAPPER.readTree("[1,2]")));
        ObjectNode huge = MAPPER.createObjectNode().put("x", "a".repeat(1_100_000));
        assertNotNull(store.write(huge));
        assertEquals(MAPPER.createObjectNode(), store.read());
    }

    @Test
    void corruptFileReadsAsEmptyObject() throws Exception {
        Files.createDirectories(dataDir.resolve("data"));
        Files.writeString(dataDir.resolve("data/editor-layout.json"), "{not json");
        assertEquals(MAPPER.createObjectNode(), new EditorLayoutStore(dataDir).read());
    }
}
