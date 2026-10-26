package dev.objz.commandbridge.backends.net.in;

import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommandResult;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExecuteCommandHandlerTest {

    private static final UUID STEVE = UUID.randomUUID();

    private record Call(ExecuteCommand command, Set<String> permissions) {
    }

    private final List<Call> calls = new ArrayList<>();
    private Supplier<CompletableFuture<CommandExecutor.ExecutionResult>> outcome =
            () -> CompletableFuture.completedFuture(CommandExecutor.ExecutionResult.success());

    private final CommandExecutor executor = new CommandExecutor() {
        @Override
        public CompletableFuture<ExecutionResult> execute(ExecuteCommand command) {
            return execute(command, null);
        }

        @Override
        public CompletableFuture<ExecutionResult> execute(ExecuteCommand command, Set<String> permissions) {
            calls.add(new Call(command, permissions));
            return outcome.get();
        }
    };

    private final ExecuteCommandHandler handler = new ExecuteCommandHandler(executor);
    private final RecordingEndpoint endpoint = new RecordingEndpoint().wire(handler);

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    private void receive(JsonNode payload) {
        handler.accept(endpoint, new Envelope(1, UUID.randomUUID(), MessageType.EXECUTE_COMMAND,
                "proxy-1", "survival-1", 0L, payload));
    }

    private void receive(ExecuteCommand command) {
        receive(Envelope.MAPPER.valueToTree(command));
    }

    private ExecuteCommandResult result() throws Exception {
        return endpoint.onlyReply(MessageType.EXECUTE_COMMAND_RESULT, ExecuteCommandResult.class);
    }

    @Test
    void missingPayloadIsAnsweredWithFailure() throws Exception {
        receive((JsonNode) null);

        ExecuteCommandResult result = result();
        assertFalse(result.success());
        assertEquals("Null payload received", result.message());
        assertTrue(calls.isEmpty());
    }

    @Test
    void nullCommandIsAnsweredWithFailure() throws Exception {
        receive(new ExecuteCommand(null, RunAs.CONSOLE, null, null));

        assertFalse(result().success());
        assertTrue(calls.isEmpty());
    }

    @Test
    void blankCommandIsAnsweredWithFailure() throws Exception {
        receive(new ExecuteCommand("   ", RunAs.CONSOLE, null, null));

        ExecuteCommandResult result = result();
        assertFalse(result.success());
        assertEquals("Empty command received", result.message());
        assertTrue(calls.isEmpty());
    }

    @Test
    void unparsablePayloadIsAnsweredWithFailure() throws Exception {
        receive(Envelope.MAPPER.valueToTree("say hi"));

        ExecuteCommandResult result = result();
        assertFalse(result.success());
        assertTrue(result.message().startsWith("Failed to parse command"), result.message());
        assertTrue(calls.isEmpty());
    }

    @Test
    void operatorPermissionsAreForwarded() {
        receive(new ExecuteCommand("gamemode creative", RunAs.OPERATOR, STEVE, Set.of("gamemode", "gamemode.*")));

        assertEquals(Set.of("gamemode", "gamemode.*"), calls.getFirst().permissions());
    }

    @Test
    void permissionsAreDroppedForPlayer() {
        receive(new ExecuteCommand("gamemode creative", RunAs.PLAYER, STEVE, Set.of("gamemode")));

        assertNull(calls.getFirst().permissions());
    }

    @Test
    void permissionsAreDroppedForConsole() {
        receive(new ExecuteCommand("say hi", RunAs.CONSOLE, null, Set.of("*")));

        assertNull(calls.getFirst().permissions());
        assertEquals("say hi", calls.getFirst().command().command());
    }

    @Test
    void successIsReported() throws Exception {
        receive(new ExecuteCommand("heal", RunAs.PLAYER, STEVE, null));

        ExecuteCommandResult result = result();
        assertTrue(result.success());
        assertEquals("heal", result.command());
        assertEquals(STEVE, result.playerUuid());
    }

    @Test
    void executorFailureIsReported() throws Exception {
        outcome = () -> CompletableFuture.completedFuture(CommandExecutor.ExecutionResult.playerOffline(STEVE));

        receive(new ExecuteCommand("heal", RunAs.PLAYER, STEVE, null));

        ExecuteCommandResult result = result();
        assertFalse(result.success());
        assertEquals("Player is offline: " + STEVE, result.message());
    }

    @Test
    void exceptionallyCompletedExecutionIsReported() throws Exception {
        outcome = () -> CompletableFuture.failedFuture(new IllegalStateException("boom"));

        receive(new ExecuteCommand("heal", RunAs.CONSOLE, null, null));

        ExecuteCommandResult result = result();
        assertFalse(result.success());
        assertTrue(result.message().contains("boom"), result.message());
    }

    @Test
    void executorThrowingIsReported() throws Exception {
        outcome = () -> {
            throw new IllegalStateException("boom");
        };

        try {
            receive(new ExecuteCommand("heal", RunAs.CONSOLE, null, null));
        } catch (RuntimeException ignored) {
            // the reply below is what matters to the proxy
        }

        ExecuteCommandResult result = result();
        assertFalse(result.success());
    }
}
