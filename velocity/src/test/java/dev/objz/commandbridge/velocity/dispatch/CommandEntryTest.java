package dev.objz.commandbridge.velocity.dispatch;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.Server;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.FakeProxy;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.net.out.ctx.ExecuteCommandContext;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import dev.objz.commandbridge.velocity.util.UserCaches;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the per-command part of {@link CommandEntry} (cooldown, delay, schedule-online and
 * player-arg resolution) through its private {@code processCommandAt}.
 */
final class CommandEntryTest {

    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-00000000a1e5");
    private static final IdMapping SURVIVAL = new IdMapping("survival-1", Location.BACKEND);

    @TempDir
    Path dir;

    private final FakeProxy proxy = new FakeProxy();
    private final SessionHub sessions = new SessionHub();
    private final PlayerTracker tracker = new PlayerTracker();
    private final List<ExecuteCommandContext> sent = new ArrayList<>();
    private final List<String> alexMessages = new ArrayList<>();
    private Function<String, UUID> mojang = name -> null;
    private CommandEntry entry;

    private static Method processCommandAt;

    @BeforeAll
    static void setUpClass() throws Exception {
        TestFixtures.ensureLog();
        processCommandAt = CommandEntry.class.getDeclaredMethod("processCommandAt",
                ExecutionContext.class, List.class, int.class);
        processCommandAt.setAccessible(true);
    }

    @BeforeEach
    void setUp() {
        OutNode outNode = new OutNode().setEndpointSendFactory((ep, env) -> null);
        outNode.register(MessageType.EXECUTE_COMMAND, new OutboundHandler<ExecuteCommandContext>() {
            @Override
            public SendOperation accept(ExecuteCommandContext context) {
                sent.add(context);
                return null;
            }
        });
        var userCache = UserCaches.offline(proxy.server, sessions, outNode, dir.resolve("usercache.json"),
                name -> mojang.apply(name));
        entry = new CommandEntry(proxy.server, new Object(), new ScriptManager(dir, PlatformFeatures.none()),
                sessions, outNode, "proxy-1", dir, tracker, userCache, PlatformFeatures.none(),
                Duration.ofDays(1));
        connect("survival-1");
    }

    private void connect(String id) {
        var session = sessions.add(id, new Endpoint() {
            @Override
            public CompletableFuture<Void> send(Envelope env) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        });
        session.location(Location.BACKEND);
        session.status(AuthStatus.AUTH_OK);
    }

    private static Script script(CmdMapping... commands) {
        return new Script(4, "give", true, null, List.of(), new Permissions(true, false),
                List.of(new IdMapping("proxy-1", Location.VELOCITY)),
                new Defaults(RunAs.CONSOLE, List.of(SURVIVAL), null, Duration.ZERO, Duration.ZERO),
                List.of(), List.of(commands));
    }

    private static CmdMapping cmd(String command, Server server, Duration delay, Duration cooldown) {
        return new CmdMapping(command, RunAs.CONSOLE, List.of(SURVIVAL), server, delay, cooldown);
    }

    private CommandSource alex() {
        return FakeProxy.player(ALEX, "Alex", alexMessages);
    }

    private void run(CommandSource source, UUID playerUuid, Map<String, Object> args, Script script)
            throws Exception {
        var ctx = new ExecutionContext(null, null, source, playerUuid, script, args, null, -1);
        processCommandAt.invoke(entry, ctx, script.commands(), 0);
    }

    private void awaitSent(int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (sent.size() < count && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    @Test
    void commandsRunInOrder() throws Exception {
        run(proxy.console, null, Map.of(), script(cmd("say one", null, null, null), cmd("say two", null, null, null)));

        assertEquals(List.of("say one", "say two"), sent.stream().map(ExecuteCommandContext::command).toList());
    }

    @Test
    void cooldownBlocksSecondUseAndStopsTheChain() throws Exception {
        Script script = script(cmd("heal", null, null, Duration.ofMinutes(1)), cmd("feed", null, null, null));

        run(alex(), ALEX, Map.of(), script);
        run(alex(), ALEX, Map.of(), script);

        assertEquals(List.of("heal", "feed"), sent.stream().map(ExecuteCommandContext::command).toList());
        assertEquals(1, alexMessages.size());
        assertTrue(alexMessages.getFirst().contains("Try again in"), alexMessages.toString());
    }

    @Test
    void cooldownDoesNotApplyToConsole() throws Exception {
        Script script = script(cmd("heal", null, null, Duration.ofMinutes(1)));

        run(proxy.console, null, Map.of(), script);
        run(proxy.console, null, Map.of(), script);

        assertEquals(2, sent.size());
    }

    @Test
    void delayedCommandGoesToProxyScheduler() throws Exception {
        int before = proxy.scheduled.size();

        run(proxy.console, null, Map.of(), script(cmd("say later", null, Duration.ofSeconds(2), null)));

        assertTrue(sent.isEmpty());
        assertEquals(before + 1, proxy.scheduled.size());
        FakeProxy.ScheduledCall call = proxy.scheduled.getLast();
        assertEquals(2000, call.delayMillis);
        assertTrue(call.scheduled);
        assertFalse(call.repeating);

        call.task.run();
        assertEquals("say later", sent.getFirst().command());
    }

    @Test
    void scheduleOnlineQueuesWhenPlayerIsNotOnTarget() throws Exception {
        tracker.addPlayer("lobby", ALEX);

        run(alex(), ALEX, Map.of(), script(cmd("kit starter", new Server(false, true, null), null, null)));

        assertTrue(sent.isEmpty());
        assertEquals(1, entry.scheduler().size());
        assertEquals(ALEX, entry.scheduler().tasks().iterator().next().playerUuid());
    }

    @Test
    void scheduleOnlineRunsImmediatelyWhenPlayerIsOnTarget() throws Exception {
        tracker.addPlayer("survival-1", ALEX);

        run(alex(), ALEX, Map.of(), script(cmd("kit starter", new Server(false, true, null), null, null)));

        assertEquals(1, sent.size());
        assertEquals(0, entry.scheduler().size());
    }

    @Test
    void scheduleOnlineWithoutPlayerRunsImmediately() throws Exception {
        run(proxy.console, null, Map.of(), script(cmd("say hi", new Server(false, true, null), null, null)));

        assertEquals(1, sent.size());
        assertEquals(0, entry.scheduler().size());
    }

    @Test
    void playerArgUuidBecomesTheCommandPlayer() throws Exception {
        UUID target = UUID.randomUUID();

        run(proxy.console, null, Map.of("target", target.toString()),
                script(cmd("heal", new Server(false, false, "target"), null, null)));

        assertEquals(target, sent.getFirst().uuid());
    }

    @Test
    void playerArgNameIsResolvedThroughUserCache() throws Exception {
        UUID steve = UUID.randomUUID();
        proxy.addPlayer(steve, "Steve");

        run(proxy.console, null, Map.of("target", "steve"),
                script(cmd("heal", new Server(false, false, "target"), null, null)));

        assertEquals(steve, sent.getFirst().uuid());
    }

    @Test
    void resolvedPlayerArgDecidesScheduling() throws Exception {
        UUID steve = UUID.randomUUID();

        run(alex(), ALEX, Map.of("target", steve.toString()),
                script(cmd("kit starter", new Server(false, true, "target"), null, null)));

        assertEquals(1, entry.scheduler().size());
        assertEquals(steve, entry.scheduler().tasks().iterator().next().playerUuid());
    }

    @Test
    void unresolvablePlayerArgKeepsTheSender() throws Exception {
        run(alex(), ALEX, Map.of("target", "nobody"),
                script(cmd("heal", new Server(false, false, "target"), null, null)));

        awaitSent(1);
        assertEquals(ALEX, sent.getFirst().uuid());
    }

    @Test
    void failingPlayerArgLookupIsReportedToSender() throws Exception {
        mojang = name -> {
            throw new IllegalStateException("mojang down");
        };

        run(alex(), ALEX, Map.of("target", "nobody"),
                script(cmd("heal", new Server(false, false, "target"), null, null)));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (alexMessages.isEmpty() && sent.isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(!alexMessages.isEmpty() || !sent.isEmpty(),
                "a failed player-arg lookup must either dispatch or tell the sender, not drop the command");
    }
}
