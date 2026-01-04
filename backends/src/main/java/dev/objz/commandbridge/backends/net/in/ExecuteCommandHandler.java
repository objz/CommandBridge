package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.net.proto.Envelope;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Objects;

public final class ExecuteCommandHandler extends InboundHandler {

	private final CommandExecutor executor;

	public ExecuteCommandHandler(CommandExecutor executor) {
		this.executor = Objects.requireNonNull(executor, "executor");
	}

	@Override
	public void accept(WebSocketChannel ch, Envelope env) {
		if (env.payload() == null) {
			Log.warn("Received EXECUTE_COMMAND with null payload from '{}'", env.from());
			return;
		}

		ExecuteCommand exec;
		try {
			exec = Envelope.MAPPER.treeToValue(env.payload(), ExecuteCommand.class);
		} catch (Exception e) {
			Log.error(e, "Failed to parse EXECUTE_COMMAND from '{}'", env.from());
			return;
		}

		if (exec == null || exec.command() == null || exec.command().isBlank()) {
			Log.warn("Received empty EXECUTE_COMMAND from '{}'", env.from());
			return;
		}

		Log.debug("Executing command '{}' as {} (player: {})",
				exec.command(),
				exec.runAs(),
				exec.uuid() != null ? exec.uuid() : "N/A");

		executor.execute(exec)
				.thenAccept(result -> {
					if (result.isSuccess()) {
						Log.debug("Command '{}' executed successfully", exec.command());
					} else {
						Log.warn("Command '{}' execution failed: {}", exec.command(),
								result.message());
					}
				})
				.exceptionally(ex -> {
					Log.error(ex, "Command '{}' execution threw exception", exec.command());
					return null;
				});
	}
}
