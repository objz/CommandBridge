package dev.objz.commandbridge.velocity.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.logging.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A JSON object the web editor keeps on the proxy: node positions and groups, or the draft of unsaved
 * changes. Kept out of the scripts so they run unchanged; the proxy only stores it, it never acts on it.
 */
public final class EditorLayoutStore {

    static final int MAX_BYTES = 1_000_000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;

    public EditorLayoutStore(Path dataDir) {
        this(dataDir, "editor-layout.json");
    }

    public EditorLayoutStore(Path dataDir, String fileName) {
        this.file = dataDir.resolve("data").resolve(fileName);
    }

    /** Another store in the same folder. */
    public EditorLayoutStore sibling(String fileName) {
        return new EditorLayoutStore(file.getParent().getParent(), fileName);
    }

    public JsonNode read() {
        try {
            if (Files.exists(file)) {
                JsonNode node = MAPPER.readTree(Files.readString(file, StandardCharsets.UTF_8));
                if (node != null && node.isObject()) {
                    return node;
                }
            }
        } catch (IOException e) {
            Log.warn("Ignoring unreadable editor layout '{}': {}", file, e.getMessage());
        }
        return MAPPER.createObjectNode();
    }

    public String write(JsonNode layout) {
        if (layout == null || !layout.isObject()) {
            return "Layout must be a JSON object";
        }
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(layout);
            if (bytes.length > MAX_BYTES) {
                return "Layout is too large";
            }
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, bytes);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return null;
        } catch (IOException e) {
            Log.error(e, "Failed to write the editor layout '{}': {}", file, e.getMessage());
            return "Could not save the layout, the proxy console has the details";
        }
    }
}
