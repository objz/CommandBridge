package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import io.undertow.websockets.core.WebSocketChannel;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ExecuteCommandHandler extends InboundHandler {

	private final JavaPlugin plugin;

	public ExecuteCommandHandler(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	//todo implement executer

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

		String command = exec.command();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}

		RunAs runAs = exec.runAs();
		if (runAs == null) {
			runAs = RunAs.CONSOLE;
		}

	}
}
