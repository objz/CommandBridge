package dev.objz.commandbridge.velocity.dispatch;

import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStoreTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static ScheduledTask task() {
        return new ScheduledTask(UUID.randomUUID(), UUID.randomUUID(), "alert", null, Map.of("player", "Steve"), 0, 1234L);
    }

    @Test
    void savedTasksLoadBack(@TempDir Path dir) {
        Path file = dir.resolve("data").resolve("tasks.json");
        ScheduledTask t = task();

        TaskStore.save(file, List.of(t));

        assertEquals(List.of(t), TaskStore.load(file));
        assertFalse(Files.exists(dir.resolve("data").resolve("tasks.json.tmp")));
    }

    @Test
    void noFileMeansNoTasks(@TempDir Path dir) {
        assertEquals(List.of(), TaskStore.load(dir.resolve("tasks.json")));
    }

    @Test
    void aBrokenFileIsKeptAsideNotOverwritten(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("tasks.json");
        Files.writeString(file, "[{\"id\": \"not closed");

        assertEquals(List.of(), TaskStore.load(file));

        Path broken = dir.resolve("tasks.json.broken");
        assertTrue(Files.exists(broken), "the unreadable file is kept for a person to look at");
        assertEquals("[{\"id\": \"not closed", Files.readString(broken));
        TaskStore.save(file, List.of());
        assertEquals("[{\"id\": \"not closed", Files.readString(broken), "a later save leaves it alone");
    }
}
