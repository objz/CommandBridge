package dev.objz.commandbridge.velocity.dispatch;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.velocity.FakeProxy;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.VelocityTestDoubles;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScheduleManagerTest {
    @TempDir Path dir;
    private final FakeProxy proxy = new FakeProxy();
    private final PlayerTracker tracker = new PlayerTracker();
    private final UUID player = UUID.randomUUID();
    private ScriptManager scripts;
    private Script script;
    private CmdMapping command;
    private ScheduleManager scheduler;

    @BeforeAll
    static void log() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setUp() throws Exception {
        scripts = new ScriptManager(dir, PlatformFeatures.none());
        var base = VelocityTestDoubles.script("alert");
        command = new CmdMapping("say authorized", RunAs.CONSOLE,
                List.of(new IdMapping("survival", Location.BACKEND)), null, null, null);
        script = withCommands(base, List.of(command));
        enabled(List.of(script));
        scheduler = new ScheduleManager(proxy.server, new Object(), dir, scripts, tracker, "proxy", Duration.ofDays(1));
    }

    private static Script withCommands(Script base, List<CmdMapping> commands) {
        return new Script(base.version(), base.name(), base.enabled(), base.description(), base.aliases(),
                base.permissions(), base.register(), base.defaults(), base.args(), commands);
    }

    private void enabled(List<Script> values) throws Exception {
        var field = ScriptManager.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.set(scripts, values);
    }

    private void queue() {
        scheduler.queueTask(new ExecutionContext(null, null, proxy.console, player, script, Map.of(), command, 0), command, 0);
    }

    @Test
    void unchangedAuthorizedRevisionResumesAfterRestart() {
        queue();
        var restoredTracker = new PlayerTracker();
        var restored = new ScheduleManager(proxy.server, new Object(), dir, scripts, restoredTracker, "proxy", Duration.ofDays(1));
        var calls = new AtomicInteger();
        restored.setExecutionCallback(ctx -> {
            assertEquals(script, ctx.script());
            assertEquals(command, ctx.currentCommand());
            calls.incrementAndGet();
        });

        restoredTracker.addPlayer("survival", player);

        assertEquals(1, calls.get());
        assertEquals(0, restored.size());
    }

    @Test
    void changedTailCannotExecuteUnderAnOldAuthorization() throws Exception {
        queue();
        enabled(List.of(withCommands(script, List.of(command,
                new CmdMapping("op intruder", RunAs.CONSOLE, command.execute(), null, null, null)))));
        var calls = new AtomicInteger();
        scheduler.setExecutionCallback(ctx -> calls.incrementAndGet());

        tracker.addPlayer("survival", player);

        assertEquals(0, calls.get());
        assertEquals(0, scheduler.size());
    }

    @Test
    void disabledInvalidAndMissingScriptsCannotResumeEvenWhenInLoadedList() throws Exception {
        queue();
        enabled(List.of());
        var loaded = ScriptManager.class.getDeclaredField("loaded");
        loaded.setAccessible(true);
        loaded.set(scripts, List.of(script));
        var calls = new AtomicInteger();
        scheduler.setExecutionCallback(ctx -> calls.incrementAndGet());

        tracker.addPlayer("survival", player);

        assertEquals(0, calls.get());
        assertEquals(0, scheduler.size());
    }

    @Test
    void legacyTasksWithoutRevisionAreReadButNeverExecuted() {
        TaskStore.save(dir.resolve("data/tasks.json"), List.of(new ScheduledTask(UUID.randomUUID(), player,
                script.name(), command, Map.of(), 0, System.currentTimeMillis())));
        var restoredTracker = new PlayerTracker();
        var restored = new ScheduleManager(proxy.server, new Object(), dir, scripts, restoredTracker, "proxy", Duration.ofDays(1));
        var calls = new AtomicInteger();
        restored.setExecutionCallback(ctx -> calls.incrementAndGet());

        restoredTracker.addPlayer("survival", player);

        assertEquals(0, calls.get());
        assertEquals(0, restored.size());
    }

    @Test
    void concurrentJoinsClaimATaskOnlyOnce() throws Exception {
        queue();
        var calls = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        scheduler.setExecutionCallback(ctx -> {
            if (calls.incrementAndGet() == 1) {
                entered.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        });
        var first = CompletableFuture.runAsync(() -> tracker.addPlayer("survival", player));
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            CompletableFuture.runAsync(() -> tracker.addPlayer("survival", player)).get(5, TimeUnit.SECONDS);
            assertEquals(1, calls.get());
            assertTrue(TaskStore.load(dir.resolve("data/tasks.json")).isEmpty(), "claim is persisted before callback");
        } finally {
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        }
        assertEquals(0, scheduler.size());
    }

    @Test
    void synchronousFailureConsumesTaskWithoutAutomaticRetry() {
        queue();
        var calls = new AtomicInteger();
        scheduler.setExecutionCallback(ctx -> {
            calls.incrementAndGet();
            throw new IllegalStateException("partially dispatched");
        });

        tracker.addPlayer("survival", player);
        tracker.addPlayer("survival", player);

        assertEquals(1, calls.get());
        assertEquals(0, scheduler.size());
        assertTrue(TaskStore.load(dir.resolve("data/tasks.json")).isEmpty());
    }

    @Test
    void aJoinBeforeCallbackInstallationDoesNotDiscardWork() {
        queue();
        tracker.addPlayer("survival", player);
        assertEquals(1, scheduler.size());
    }
}
