package dev.objz.commandbridge.velocity.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.velocity.exec.CommandExecutor;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Objects;

public final class InvokedCommandHandler extends InboundHandler {

	private final SessionHub sessions;
	private final CommandExecutor executor;

	public InvokedCommandHandler(SessionHub sessions, CommandExecutor executor) {
		this.sessions = Objects.requireNonNull(sessions);
		this.executor = Objects.requireNonNull(executor);
	}

	@Override
	public void accept(WebSocketChannel ch, Envelope env) {
		if (env.payload() == null) {
			Log.warn("Received INVOKED_COMMAND with null payload from '{}'", env.from());
			return;
		}

		InvokedCommand invoked;
		try {
			invoked = Envelope.MAPPER.treeToValue(env.payload(), InvokedCommand.class);
		} catch (Exception e) {
			Log.error(e, "Failed to parse INVOKED_COMMAND from '{}'", env.from());
			return;
		}

		if (invoked == null) {
			Log.warn("Parsed INVOKED_COMMAND is null from '{}'", env.from());
			return;
		}

		ClientSession session = sessions.get(ch);
		if (session == null) {
			Log.warn("No session found for channel when handling INVOKED_COMMAND");
			return;
		}

		Log.debug("Received INVOKED_COMMAND '{}' from '{}' with {} args",
				invoked.name(),
				env.from(),
				invoked.args() != null ? invoked.args().size() : 0);

		try {
			executor.execute(invoked, session);
		} catch (Exception e) {
			Log.error(e, "Failed to execute invoked command '{}'", invoked.name());
		}
	}
}
