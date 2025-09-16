package dev.objz.commandbridge.backends.ws.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.objz.commandbridge.backends.PlatformInterface;
import dev.objz.commandbridge.backends.api.PlatformRegistry;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.backends.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.proto.cmd.RegisterCommandsResultPayload;

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

		// Accept both ArrayList payload and Record
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

		PlatformRegistry.RegistrationResult rr;
		try {
			rr = platform.platformRegistry().registerAll(reload, new ArrayList<>(stubs));
		} catch (Throwable t) {
			Log.error(t, "REGISTER_COMMANDS failed");
			rr = new PlatformRegistry.RegistrationResult(
					stubs.size(), 0, stubs.size(), List.of(), List.of(t.getMessage()));
		}

		final int requested = rr.requested();
		final int registered = rr.registered();
		final int failed = rr.failed();
		final int warnCount = rr.warnings() == null ? 0 : rr.warnings().size();
		final int errCount = rr.errors() == null ? 0 : rr.errors().size();

		if (warnCount > 0) {
			for (String w : rr.warnings()) {
				Log.info(String.format("%sWarn:%s %s", Log.YELLOW, Log.RESET, w));
			}
		}
		if (errCount > 0) {
			for (String e : rr.errors()) {
				Log.info(String.format("%sError:%s %s", Log.RED, Log.RESET, e));
			}
		}

		RegisterCommandsResultPayload ackPayload = new RegisterCommandsResultPayload(
				requested, registered, failed, rr.warnings(), rr.errors());
		ws.send(Envelope.reply(env, MessageType.REGISTER_COMMANDS_RESULT, ws.clientId(),
				mapper.valueToTree(ackPayload)));
	}
}
