package dev.objz.commandbridge.backends.ws.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.objz.commandbridge.backends.PlatformInterface;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.backends.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.main.logging.FeedbackLog;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.proto.feedback.Feedback;

import java.util.ArrayList;
import java.util.List;

public final class RegisterCommandsHandler implements InboundHandler {
	private final WsClient ws;
	private final ObjectMapper mapper;
	private final PlatformInterface platform;

	public RegisterCommandsHandler(WsClient ws, ObjectMapper mapper, PlatformInterface platform) {
		this.ws = ws;
		this.mapper = mapper;
		this.platform = platform;
	}

	@Override
	public void handle(Envelope env) throws Exception {
		JsonNode p = env.payload();
		boolean reload = false;
		List<CommandStub> stubs = List.of();

		// Accept both array payload and { reload, commands } record payload
		if (p.isArray()) {
			stubs = mapper.convertValue(p, new TypeReference<List<CommandStub>>() {
			});
		} else if (p.isObject()) {
			reload = p.path("reload").asBoolean(false);
			JsonNode arr = p.path("commands");
			if (arr.isArray()) {
				stubs = mapper.convertValue(arr, new TypeReference<List<CommandStub>>() {
				});
			}
		}

		Feedback fb;
		try {
			fb = platform.platformRegistry().registerAll(reload, new ArrayList<>(stubs));
		} catch (Throwable t) {
			Log.error(t, "REGISTER_COMMANDS failed");
			fb = new Feedback(stubs.size(), 0, stubs.size(), List.of(), List.of(t.getMessage()));
		}

		FeedbackLog.summary("Register", fb);
		FeedbackLog.details(fb);

		ws.send(Envelope.reply(env, MessageType.FEEDBACK, ws.clientId(),
				mapper.valueToTree(fb)));
	}
}
