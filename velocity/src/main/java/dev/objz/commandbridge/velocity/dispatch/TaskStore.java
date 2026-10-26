package dev.objz.commandbridge.velocity.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** tasks.json: queued commands that wait for a player, kept across restarts. */
final class TaskStore {

    private TaskStore() {
    }

    /**
     * The tasks in {@code file}; none when there is no file. A file that can't be read is moved to
     * tasks.json.broken first, so the next save doesn't overwrite the only copy of those tasks.
     */
    static List<ScheduledTask> load(Path file) {
        if (Files.notExists(file))
            return List.of();
        try {
            return Envelope.MAPPER.readValue(file.toFile(), new TypeReference<List<ScheduledTask>>() {
            });
        } catch (IOException e) {
            Path broken = file.resolveSibling(file.getFileName() + ".broken");
            Log.error("Could not read '{}': {}. It is kept as '{}', queued tasks start empty", file.getFileName(), e.getMessage(),
                    broken.getFileName());
            try {
                Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveError) {
                Log.error("Could not move '{}' aside: {}", file.getFileName(), moveError.getMessage());
            }
            return List.of();
        }
    }

    /** Writes a temp file and moves it over, so a crash mid-write never leaves half a file. */
    static void save(Path file, Collection<ScheduledTask> tasks) {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Envelope.MAPPER.writeValue(tmp.toFile(), new ArrayList<>(tasks));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Log.error("Failed to save scheduled tasks: {}", e.getMessage());
        }
    }
}
