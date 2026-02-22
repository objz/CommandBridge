package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommandResult;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ExecuteCommandHandler extends InboundHandler {

    private final CommandExecutor executor;

    public ExecuteCommandHandler(CommandExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void accept(WebSocketChannel ch, Envelope env) {
        if (env.payload() == null) {
            Log.warn("Received EXECUTE_COMMAND with null payload from '{}'", env.from());
            sendFailure(ch, env, null, null, "Null payload received");
            return;
        }

        ExecuteCommand exec;
        try {
            exec = Envelope.MAPPER.treeToValue(env.payload(), ExecuteCommand.class);
        } catch (Exception e) {
            Log.error(e, "Failed to parse EXECUTE_COMMAND from '{}'", env.from());
            sendFailure(ch, env, null, null, "Failed to parse command:  " + e.getMessage());
            return;
        }

        if (exec == null || exec.command() == null || exec.command().isBlank()) {
            Log.warn("Received empty EXECUTE_COMMAND from '{}'", env.from());
            sendFailure(ch, env, "", null, "Empty command received");
            return;
        }

        Log.debug("Executing command '{}' as {} (player: {})",
                exec.command(),
                exec.runAs(),
                exec.uuid() != null ? exec.uuid() : "N/A");

        Set<String> grantedPermissions = null;
        if (exec.runAs() == RunAs.OPERATOR && exec.grantedPermissions() != null) {
            grantedPermissions = new HashSet<>(exec.grantedPermissions());
        }

        final Set<String> finalPermissions = grantedPermissions;

        executor.execute(exec, finalPermissions)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        Log.debug("Command '{}' executed successfully", exec.command());
                        sendSuccess(ch, env, exec.command(), exec.uuid());
                    } else {
                        Log.warn("Command '{}' execution failed: {}", exec.command(),
                                result.message());
                        sendFailure(ch, env, exec.command(), exec.uuid(), result.message());
                    }
                })
                .exceptionally(ex -> {
                    Log.error(ex, "Command '{}' execution threw exception", exec.command());
                    sendFailure(ch, env, exec.command(), exec.uuid(),
                            "Exception:  " + ex.getMessage());
                    return null;
                });
    }

    private void sendSuccess(WebSocketChannel ch, Envelope env, String command, java.util.UUID playerUuid) {
        ExecuteCommandResult result = ExecuteCommandResult.success(command, playerUuid);
        reply(ch, env, MessageType.EXECUTE_COMMAND_RESULT, result)
                .dispatch()
                .exceptionally(ex -> {
                    Log.warn("Failed to send execution result: {}", ex.toString());
                    return null;
                });
    }

    private void sendFailure(WebSocketChannel ch, Envelope env, String command, java.util.UUID playerUuid,
            String message) {
        ExecuteCommandResult result = ExecuteCommandResult.failure(command, playerUuid, message);
        reply(ch, env, MessageType.EXECUTE_COMMAND_RESULT, result)
                .dispatch()
                .exceptionally(ex -> {
                    Log.warn("Failed to send execution result: {}", ex.toString());
                    return null;
                });
    }
}
