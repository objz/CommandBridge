package dev.objz.commandbridge.velocity.dispatch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.cmd.ref.EntityRef;
import dev.objz.commandbridge.cmd.ref.Location2D;
import dev.objz.commandbridge.cmd.ref.Location3D;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.VelocityTestDoubles;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void complexArgumentsAndAuthorizedRevisionSurviveRestart(@TempDir Path dir) {
        var base = VelocityTestDoubles.script("alert");
        var script = new Script(base.version(), base.name(), base.enabled(),
                base.description(), base.aliases(), base.permissions(), base.register(), base.defaults(),
                List.of(new ArgMapping("player", true, ArgType.PLAYERS, null),
                        new ArgMapping("location", true, ArgType.LOCATION, null),
                        new ArgMapping("location2d", true, ArgType.LOCATION_2D, null)), base.commands());
        var args = Map.<String, Object>of("player", List.of(new EntityRef("PLAYER", "uuid", "Alex")),
                "location", new Location3D("world", 1, 2, 3), "location2d", new Location2D("world", 4, 5));
        var task = new ScheduledTask(UUID.randomUUID(), UUID.randomUUID(), script.name(), script.commands().getFirst(),
                args, 0, System.currentTimeMillis(), script);
        Path file = dir.resolve("tasks.json");

        TaskStore.save(file, List.of(task));

        assertEquals(List.of(task), TaskStore.load(file));
    }

    @Test
    void legacyTasksRemainReadableWithoutAuthorization(@TempDir Path dir) throws Exception {
        var old = Envelope.MAPPER.valueToTree(task());
        ((ObjectNode) old).remove("authorizedScript");
        Path file = dir.resolve("tasks.json");
        Files.writeString(file, "[" + old + "]");

        assertEquals(1, TaskStore.load(file).size());
        assertNull(TaskStore.load(file).getFirst().authorizedScript());
        assertFalse(Files.exists(dir.resolve("tasks.json.broken")));
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
