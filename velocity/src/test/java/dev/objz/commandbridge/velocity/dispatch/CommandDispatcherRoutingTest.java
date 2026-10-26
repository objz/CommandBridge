package dev.objz.commandbridge.velocity.dispatch;

import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.Server;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.FakeProxy;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.VelocityTestDoubles;
import dev.objz.commandbridge.velocity.dispatch.exec.LocalDispatcher;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.net.out.ctx.ExecuteCommandContext;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CommandDispatcherRoutingTest {

    private static final String LOCAL_ID = "proxy-1";
    private static final UUID PLAYER = UUID.randomUUID();

    private final FakeProxy proxy = new FakeProxy();
    private final SessionHub sessions = new SessionHub();
    private final PlayerTracker tracker = new PlayerTracker();
    private final List<ExecuteCommandContext> sent = new ArrayList<>();
    private final FakeProxy.RecordingSource source = new FakeProxy.RecordingSource();
    private CommandDispatcher dispatcher;

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
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
        dispatcher = new CommandDispatcher(sessions, outNode, new LocalDispatcher(proxy.server, LOCAL_ID), tracker);
    }

    private void connect(String id, Location location) {
        Endpoint endpoint = new Endpoint() {
            @Override
            public CompletableFuture<Void> send(dev.objz.commandbridge.net.proto.Envelope env) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        };
        var session = sessions.add(id, endpoint);
        session.location(location);
        session.status(AuthStatus.AUTH_OK);
    }

    private ExecutionContext context(UUID player) {
        return VelocityTestDoubles.context(VelocityTestDoubles.script("give"), source, new String[0])
                .withPlayerUuid(player);
    }

    private static CmdMapping cmd(String command, RunAs runAs, Server server, IdMapping... targets) {
        return new CmdMapping(command, runAs, List.of(targets), server, null, null);
    }

    @Test
    void commandWithoutTargetsReportsErrorToSource() {
        dispatcher.dispatchCommand(context(null), new CmdMapping("say hi", null, null, null, null, null));

        assertTrue(sent.isEmpty());
        assertEquals(1, source.messages.size());
        assertTrue(source.messages.getFirst().contains("No execution targets"), source.messages.toString());
    }

    @Test
    void disconnectedTargetReportsErrorToSource() {
        dispatcher.dispatchCommand(context(null),
                cmd("say hi", RunAs.CONSOLE, null, new IdMapping("survival-1", Location.BACKEND)));

        assertTrue(sent.isEmpty());
        assertEquals(1, source.messages.size());
        assertTrue(source.messages.getFirst().contains("BACKEND server 'survival-1' is not connected"),
                source.messages.toString());
    }

    @Test
    void sessionWithOtherLocationIsNotFound() {
        connect("lobby", Location.BACKEND);

        dispatcher.dispatchCommand(context(null),
                cmd("say hi", RunAs.CONSOLE, null, new IdMapping("lobby", Location.VELOCITY)));

        assertTrue(sent.isEmpty());
        assertTrue(source.messages.getFirst().contains("VELOCITY server 'lobby' is not connected"),
                source.messages.toString());
    }

    @Test
    void targetRequiredSkipsWhenPlayerIsElsewhere() {
        connect("survival-1", Location.BACKEND);
        tracker.addPlayer("lobby", PLAYER);

        dispatcher.dispatchCommand(context(PLAYER), cmd("heal", RunAs.CONSOLE, new Server(true, false, null),
                new IdMapping("survival-1", Location.BACKEND)));

        assertTrue(sent.isEmpty());
        assertTrue(source.messages.isEmpty());
    }

    @Test
    void targetRequiredDispatchesWhenPlayerIsOnTarget() {
        connect("survival-1", Location.BACKEND);
        tracker.addPlayer("survival-1", PLAYER);

        dispatcher.dispatchCommand(context(PLAYER), cmd("heal", RunAs.CONSOLE, new Server(true, false, null),
                new IdMapping("survival-1", Location.BACKEND)));

        assertEquals(1, sent.size());
        assertEquals(PLAYER, sent.getFirst().uuid());
    }

    @Test
    void operatorReceivesGrantedPermissions() {
        connect("survival-1", Location.BACKEND);

        dispatcher.dispatchCommand(context(PLAYER),
                cmd("gamemode creative", RunAs.OPERATOR, null, new IdMapping("survival-1", Location.BACKEND)));

        ExecuteCommandContext ctx = sent.getFirst();
        assertEquals(RunAs.OPERATOR, ctx.runAs());
        assertEquals(Set.of("commandbridge.command.give", "gamemode", "gamemode.*"), ctx.grantedPermissions());
        assertEquals("survival-1", ctx.session().id());
    }

    @Test
    void consoleSendsNoPermissions() {
        connect("survival-1", Location.BACKEND);

        dispatcher.dispatchCommand(context(null),
                cmd("say hi", RunAs.CONSOLE, null, new IdMapping("survival-1", Location.BACKEND)));

        assertEquals(RunAs.CONSOLE, sent.getFirst().runAs());
        assertNull(sent.getFirst().grantedPermissions());
    }

    @Test
    void missingRunAsDefaultsToConsole() {
        connect("survival-1", Location.BACKEND);

        dispatcher.dispatchCommand(context(null),
                cmd("say hi", null, null, new IdMapping("survival-1", Location.BACKEND)));

        assertEquals(RunAs.CONSOLE, sent.getFirst().runAs());
        assertNull(sent.getFirst().grantedPermissions());
    }

    @Test
    void localVelocityTargetRunsOnThisProxy() {
        dispatcher.dispatchCommand(context(null),
                cmd("/alert hi", RunAs.CONSOLE, null, new IdMapping(LOCAL_ID, Location.VELOCITY)));

        assertTrue(sent.isEmpty());
        assertEquals(List.of(new FakeProxy.Execution(proxy.console, "alert hi")), proxy.executions);
    }

    @Test
    void localIdWithBackendLocationIsNotRunLocally() {
        dispatcher.dispatchCommand(context(null),
                cmd("say hi", RunAs.CONSOLE, null, new IdMapping(LOCAL_ID, Location.BACKEND)));

        assertTrue(proxy.executions.isEmpty());
        assertTrue(source.messages.getFirst().contains("is not connected"), source.messages.toString());
    }

    @Test
    void everyTargetIsDispatched() {
        connect("survival-1", Location.BACKEND);
        connect("creative", Location.BACKEND);

        dispatcher.dispatchCommand(context(null), cmd("say hi", RunAs.CONSOLE, null,
                new IdMapping("survival-1", Location.BACKEND), new IdMapping("creative", Location.BACKEND)));

        assertEquals(List.of("survival-1", "creative"), sent.stream().map(c -> c.session().id()).toList());
    }
}
