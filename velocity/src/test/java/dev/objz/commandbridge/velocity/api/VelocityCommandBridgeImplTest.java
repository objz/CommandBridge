package dev.objz.commandbridge.velocity.api;

import com.velocitypowered.api.proxy.Player;
import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.command.CommandPayload;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.FakeProxy;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.dispatch.exec.LocalDispatcher;
import dev.objz.commandbridge.velocity.net.EndpointServer;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class VelocityCommandBridgeImplTest {
    public record Notice(String value) implements ChannelPayload {
    }

    private final FakeProxy proxy = new FakeProxy();
    private final List<Envelope> replies = new ArrayList<>();
    private final Endpoint remote = new Endpoint() {
        public CompletableFuture<Void> send(Envelope env) {
            replies.add(env);
            return CompletableFuture.completedFuture(null);
        }

        public boolean isOpen() {
            return true;
        }
    };
    private final EndpointServer server = new EndpointServer() {
        public void start() { }
        public void stop() { }
        public void close(Endpoint endpoint) { }

        public SendOperation send(Endpoint endpoint, Envelope request) {
            return new SendOperation(endpoint, request, new ResponseAwaiter());
        }
    };
    private final VelocityCommandBridgeImpl api = new VelocityCommandBridgeImpl(new SessionHub(),
            new PlayerTracker(), "proxy-1", server, new PluginMessageQueue(),
            new LocalDispatcher(proxy.server, "proxy-1"));

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void remoteCommandExecutesWithoutUserChannelRegistration() {
        api.handlePluginMessageRequest(remote, message(new CommandPayload("say hello", RunAs.CONSOLE), true));

        assertEquals(1, proxy.executions.size());
        assertEquals("say hello", proxy.executions.getFirst().command());
        assertSame(proxy.console, proxy.executions.getFirst().source());
        assertEquals(MessageType.PLUGIN_MESSAGE_RESPONSE, replies.getFirst().type());
    }

    @Test
    void localSendsAndRequestsReusePlatformExecutorAndKeepListeners() {
        UUID uuid = UUID.randomUUID();
        Player player = proxy.addPlayer(uuid, "Steve");
        List<CommandPayload> received = new ArrayList<>();
        var channel = api.channel(CommandPayload.class);
        channel.listen((context, payload) -> received.add(payload));
        CommandPayload command = new CommandPayload("home", RunAs.PLAYER, uuid);

        channel.to(List.of(api.server())).send(command).join();
        assertEquals(command, channel.to(List.of(api.server())).request(command).join());

        assertEquals(List.of(command, command), received);
        assertEquals(2, proxy.executions.size());
        assertSame(player, proxy.executions.getFirst().source());
        assertSame(player, proxy.executions.getLast().source());
    }

    @Test
    void remotePlayerAndOperatorCommandsNeverUseConsole() {
        UUID uuid = UUID.randomUUID();
        Player player = proxy.addPlayer(uuid, "Steve");
        for (RunAs runAs : List.of(RunAs.PLAYER, RunAs.OPERATOR)) {
            api.handlePluginMessageRequest(remote, message(new CommandPayload("home", runAs, uuid), false));
        }
        assertEquals(2, proxy.executions.size());
        proxy.executions.forEach(execution -> assertSame(player, execution.source()));
    }

    @Test
    void invalidOrOfflinePlayerCommandsCannotFallBackToConsole() {
        for (CommandPayload invalid : List.of(
                new CommandPayload(null, RunAs.CONSOLE), new CommandPayload(" ", RunAs.CONSOLE),
                new CommandPayload("/say hello", RunAs.CONSOLE), new CommandPayload("say hello", null),
                new CommandPayload("home", RunAs.PLAYER), new CommandPayload("home", RunAs.OPERATOR),
                new CommandPayload("home", RunAs.PLAYER, UUID.randomUUID()),
                new CommandPayload("home", RunAs.OPERATOR, UUID.randomUUID()))) {
            api.handlePluginMessageRequest(remote, message(invalid, false));
            api.channel(CommandPayload.class).to(List.of(api.server())).send(invalid).join();
        }

        assertEquals(List.of(), proxy.executions);
    }

    @Test
    void genericUserListenersStillReceiveLocalAndRemoteMessages() {
        List<Notice> received = new ArrayList<>();
        api.channel(Notice.class).listen((context, payload) -> received.add(payload));

        api.handlePluginMessageRequest(remote, message(new Notice("remote"), false));
        api.channel(Notice.class).to(List.of(api.server())).send(new Notice("local")).join();

        assertEquals(List.of(new Notice("remote"), new Notice("local")), received);
        assertEquals(List.of(), proxy.executions);
    }

    private static Envelope message(ChannelPayload payload, boolean response) {
        PluginMessage message = new PluginMessage(payload.getClass().getName(), Envelope.MAPPER.valueToTree(payload),
                response, null, null, null);
        return Envelope.make(MessageType.PLUGIN_MESSAGE, "survival-1", "proxy-1", Envelope.MAPPER.valueToTree(message));
    }
}
