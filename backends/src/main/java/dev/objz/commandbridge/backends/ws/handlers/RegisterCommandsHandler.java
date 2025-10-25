package dev.objz.commandbridge.backends.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.backends.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.backends.platform.cmd.ArgumentMapper;
import dev.objz.commandbridge.backends.platform.cmd.CommandRegistry;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;
import dev.objz.commandbridge.proto.cmd.payloads.RegisterCommands;
import dev.objz.commandbridge.proto.feedback.Feedback;
import dev.objz.commandbridge.proto.feedback.FeedbackCollector;

public final class RegisterCommandsHandler implements InboundHandler {
	private final ObjectMapper mapper;
	private final CommandRegistry registry;
	private final WsClient ws;

	public RegisterCommandsHandler(ObjectMapper mapper, WsClient ws) {
		this.mapper = mapper;
		this.ws = ws;
		this.registry = new CommandRegistry(new ArgumentMapper());
	}

	@Override
	public void handle(Envelope env) {
		try {
			RegisterCommands payload = mapper.treeToValue(env.payload(),
					RegisterCommands.class);

			FeedbackCollector collector = new FeedbackCollector();

			try {
				registry.unregisterAll();
			} catch (Exception e) {
				Log.error(e, "Failed to unregister all commands");
				collector.warn("Failed to unregister existing commands: " + e.getMessage());
			}

			if (payload.commands() == null || payload.commands().isEmpty()) {
				Log.warn("RegisterCommands payload contains no commands");
			} else {
				for (var stub : payload.commands()) {
					try {
						registry.register(stub);
						collector.success();
					} catch (Exception e) {
						Log.error(e, "Failed to register command '{}'", stub.name());
						collector.failure("Command '" + stub.name() + "': " + e.getMessage());
					}
				}
			}

			Feedback feedback = collector.build();

			if (collector.succeeded() > 0) {
				Log.success(true, "Registered '{}' command(s)", collector.succeeded());
			}

			sendFeedback(env, feedback);

		} catch (Exception e) {
			Log.error(e, "Failed to process REGISTER_COMMANDS");
			FeedbackCollector errorCollector = new FeedbackCollector();
			errorCollector.failure("Internal error: " + e.getMessage());
			sendFeedback(env, errorCollector.build());
		}
	}

	private void sendFeedback(Envelope req, Feedback feedback) {
		try {
			var reply = Envelope.reply(req,
					MessageType.FEEDBACK,
					ws.clientId(),
					mapper.valueToTree(feedback));
			ws.send(reply);
		} catch (Exception e) {
			Log.error(e, "Failed to send feedback response");
		}
	}
}
