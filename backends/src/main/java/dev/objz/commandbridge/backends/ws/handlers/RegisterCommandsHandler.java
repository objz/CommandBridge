package dev.objz.commandbridge.backends.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.backends.PlatformRegistry;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.backends.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.logging.FeedbackLog;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.proto.cmd.RegisterCommandsPayload;
import dev.objz.commandbridge.proto.feedback.Feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RegisterCommandsHandler implements InboundHandler {
	private final WsClient ws;
	private final ObjectMapper mapper;
	private final Supplier<PlatformRegistry> platform;

	public RegisterCommandsHandler(WsClient ws, ObjectMapper mapper, Supplier<PlatformRegistry> platform) {
		this.ws = ws;
		this.mapper = mapper;
		this.platform = platform;
	}

	@Override
	public void handle(Envelope env) throws Exception {
		RegisterCommandsPayload payload;
		try {
			payload = mapper.treeToValue(env.payload(), RegisterCommandsPayload.class);
		} catch (Exception e) {
			Log.error(e, "REGISTER_COMMANDS payload invalid");
			var fb = new Feedback(0, 0, 0, List.of(), List.of("Invalid register payload"));
			// FeedbackLog.summary("Register", fb, env.from());
			FeedbackLog.details(fb, null, true);
			ws.send(Envelope.reply(env, MessageType.FEEDBACK, ws.clientId(), mapper.valueToTree(fb)));
			return;
		}

		List<CommandStub> stubs = (payload.commands() != null
				? payload.commands()
				: List.of());

		Feedback fb;
		try {
			PlatformRegistry reg = platform.get(); 
			if (reg == null) {
				fb = new Feedback(stubs.size(), 0, stubs.size(), List.of(),
						List.of("Platform not initialized"));
			} else {
				fb = reg.registerAll(payload.reload(), new ArrayList<>(stubs));
			}
		} catch (Throwable t) {
			Log.error(t, "REGISTER_COMMANDS failed");
			fb = new Feedback(stubs.size(), 0, stubs.size(), List.of(), List.of(t.getMessage()));
		}

		// FeedbackLog.summary("Register", fb, env.from());
		FeedbackLog.details(fb, null, true);

		ws.send(Envelope.reply(env, MessageType.FEEDBACK, ws.clientId(), mapper.valueToTree(fb)));
	}
}
