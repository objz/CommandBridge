package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.backends.platform.cmd.ArgumentMapper;
import dev.objz.commandbridge.backends.platform.cmd.CommandRegistry;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.RegisterCommands;
import dev.objz.commandbridge.net.payloads.feedback.Feedback;
import dev.objz.commandbridge.net.payloads.feedback.FeedbackCollector;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Objects;

public final class RegistrationHandler extends InboundHandler {
	private final WsClient ws;
	private final CommandRegistry registry;

	public RegistrationHandler(WsClient ws) {
		this.ws = Objects.requireNonNull(ws);
		this.registry = new CommandRegistry(new ArgumentMapper(), ws.outboundRouter());
	}

	@Override
	public void accept(WebSocketChannel ch, Envelope env) {
		RegisterCommands rc = null;
		try {
			rc = Envelope.MAPPER.treeToValue(env.payload(), RegisterCommands.class);
		} catch (Exception e) {
			Log.error(e, "Failed to handle REGISTER_COMMANDS from {}", env.from());
		}
		if (rc == null || rc.commands() == null || rc.commands().isEmpty()) {
			Log.warn("Received empty REGISTER_COMMANDS");
			Feedback f = Feedback.empty();
			reply(ch, env, MessageType.REGISTER_COMMANDS_RESULT, f)
					.dispatch()
					.exceptionally(ex -> {
						Log.warn("Failed to send FEEDBACK: {}", ex.toString());
						return null;
					});
			return;
		}

		try {
			registry.unregisterAll();
			Log.info("Unregistered all previous commands before reload");
		} catch (Exception e) {
			Log.warn("Failed to unregister previous commands: {}", e.getMessage());
		}

		FeedbackCollector fc = new FeedbackCollector();
		for (CommandStub s : rc.commands()) {
			try {
				registry.register(s);
				fc.success();
			} catch (Throwable t) {
				Log.error(t, "Registration failed for '{}'", s != null ? s.name() : "<null>");
				fc.failure("register '" + (s != null ? s.name() : "<null>") + "': " + t.getMessage());
			}
		}
		ws.setServerId(env.from());
		Feedback f = fc.build();
		Summary.feedbackSummary("Registration", f, env.from());
		Summary.feedbackDetails(f, env.from(), true);

		reply(ch, env, MessageType.REGISTER_COMMANDS_RESULT, f)
				.dispatch()
				.exceptionally(ex -> {
					Log.warn("Failed to send FEEDBACK: {}", ex.toString());
					return null;
				});
	}
}
