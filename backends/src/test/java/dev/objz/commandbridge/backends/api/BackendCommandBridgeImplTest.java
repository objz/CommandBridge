package dev.objz.commandbridge.backends.api;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.command.CommandPayload;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class BackendCommandBridgeImplTest {
    public record Notice(String value) implements ChannelPayload {
    }

    private final List<Envelope> sent = new ArrayList<>();
    private final List<ExecuteCommand> executed = new ArrayList<>();
    private final ResponseAwaiter awaiter = new ResponseAwaiter();
    private final AtomicReference<String> proxyId = new AtomicReference<>();
    private final Endpoint endpoint = new Endpoint() {
        public CompletableFuture<Void> send(Envelope env) {
            sent.add(env);
            awaiter.signal(Envelope.reply(env, MessageType.PLUGIN_MESSAGE_RESPONSE, env.to(), env.payload()));
            return CompletableFuture.completedFuture(null);
        }

        public boolean isOpen() {
            return true;
        }
    };
    private final BackendClient client = (BackendClient) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] { BackendClient.class }, (self, method, args) ->
                    switch (method.getName()) {
                        case "serverId" -> proxyId.get();
                        case "setServerId" -> {
                            proxyId.set((String) args[0]);
                            yield null;
                        }
                        case "send" -> new SendOperation(endpoint, (Envelope) args[0], awaiter);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
    private final CommandExecutor executor = new CommandExecutor() {
        public CompletableFuture<ExecutionResult> execute(ExecuteCommand command) {
            executed.add(command);
            return CompletableFuture.completedFuture(ExecutionResult.success());
        }

        public CompletableFuture<ExecutionResult> execute(ExecuteCommand command, Set<String> permissions) {
            return execute(command);
        }
    };
    private final PlatformAdapter<?> adapter = (PlatformAdapter<?>) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] { PlatformAdapter.class }, (self, method, args) -> {
                if (method.getName().equals("getCommandExecutor")) {
                    return executor;
                }
                throw new UnsupportedOperationException(method.getName());
            });
    private final BackendCommandBridgeImpl api = new BackendCommandBridgeImpl(client, "survival-1", adapter);

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void configuredIdentityDoesNotBecomeTheLearnedProxyIdentity() {
        assertEquals("survival-1", api.server().id());
        client.setServerId("proxy-1");
        assertEquals("survival-1", api.server().id());
        List<Notice> received = new ArrayList<>();
        api.channel(Notice.class).listen((context, payload) -> received.add(payload));

        api.channel(Notice.class).to(List.of(Platform.backend("lobby"))).send(new Notice("send")).join();
        api.channel(Notice.class).to(List.of(Platform.backend("lobby"))).request(new Notice("request")).join();
        api.handlePluginMessageRequest(message("survival-1", new Notice("inbound")));
        api.handlePluginMessageRequest(message("proxy-1", new Notice("wrong target")));

        assertEquals(List.of("survival-1", "survival-1"), sent.stream().map(Envelope::from).toList());
        assertEquals(List.of(new Notice("inbound")), received);
    }

    @Test
    void builtInCommandsExecuteWithoutUserRegistrationAndKeepUserListeners() {
        UUID player = UUID.randomUUID();
        CommandPayload console = new CommandPayload("say hello", RunAs.CONSOLE);
        api.handlePluginMessageRequest(message("survival-1", console));
        List<CommandPayload> received = new ArrayList<>();
        api.channel(CommandPayload.class).listen((context, payload) -> received.add(payload));
        CommandPayload asPlayer = new CommandPayload("home", RunAs.PLAYER, player);
        api.handlePluginMessageRequest(message("survival-1", asPlayer));
        api.handlePluginMessageRequest(message("survival-1", new CommandPayload("home", RunAs.OPERATOR, player)));

        assertEquals(List.of(RunAs.CONSOLE, RunAs.PLAYER, RunAs.OPERATOR),
                executed.stream().map(ExecuteCommand::runAs).toList());
        assertNull(executed.getFirst().uuid());
        assertEquals(player, executed.get(1).uuid());
        assertEquals(2, received.size());
    }

    @Test
    void invalidCommandsNeverReachThePlatformExecutor() {
        for (CommandPayload invalid : List.of(
                new CommandPayload(null, RunAs.CONSOLE), new CommandPayload(" ", RunAs.CONSOLE),
                new CommandPayload("/say hello", RunAs.CONSOLE), new CommandPayload("say hello", null),
                new CommandPayload("home", RunAs.PLAYER), new CommandPayload("home", RunAs.OPERATOR))) {
            api.handlePluginMessageRequest(message("survival-1", invalid));
        }
        assertEquals(List.of(), executed);
    }

    private static Envelope message(String target, ChannelPayload payload) {
        PluginMessage message = new PluginMessage(payload.getClass().getName(), Envelope.MAPPER.valueToTree(payload),
                false, null, null, null);
        return Envelope.make(MessageType.PLUGIN_MESSAGE, "proxy-1", target, Envelope.MAPPER.valueToTree(message));
    }
}
