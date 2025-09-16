package dev.objz.commandbridge.velocity.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.cmd.RegisterCommandsResultPayload;
import dev.objz.commandbridge.velocity.ws.MessageRouter.InboundHandler;
import io.undertow.websockets.core.WebSocketChannel;

public final class RegisterCommandsResultHandler implements InboundHandler {
	private final ObjectMapper mapper;

	public RegisterCommandsResultHandler(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void handle(WebSocketChannel ch, Envelope env) throws Exception {
		RegisterCommandsResultPayload r = mapper.treeToValue(env.payload(),
				RegisterCommandsResultPayload.class);

		final int requested = r.requested();
		final int registered = r.registered();
		final int failed = r.failed();
		final int warnCount = r.warnings() == null ? 0 : r.warnings().size();
		final int errCount = r.errors() == null ? 0 : r.errors().size();

		Log.info(String.format("%sBackend:%s %s'%s'%s", Log.GRAY, Log.RESET, Log.GRAY, env.from(), Log.RESET));

		final String summary = String.format(
				"%sRegister:%s requested %s%d%s, registered %s%d%s, failed %s%d%s, warnings %s%d%s, errors %s%d%s",
				Log.GRAY, Log.RESET,
				Log.GRAY, requested, Log.RESET,
				(registered > 0 ? Log.GREEN : Log.GRAY), registered, Log.RESET,
				(failed > 0 ? Log.RED : Log.GREEN), failed, Log.RESET,
				(warnCount > 0 ? Log.YELLOW : Log.GRAY), warnCount, Log.RESET,
				(errCount > 0 ? Log.RED : Log.GREEN), errCount, Log.RESET);
		Log.info(summary);

		if (warnCount > 0) {
			for (String w : r.warnings()) {
				Log.info(String.format("%sWarn:%s %s", Log.YELLOW, Log.RESET, w));
			}
		}
		if (errCount > 0) {
			for (String e : r.errors()) {
				Log.info(String.format("%sError:%s %s", Log.RED, Log.RESET, e));
			}
		}
	}
}
