package dev.objz.commandbridge.backends.ws.handlers;

import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.backends.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.Envelope;

public final class ErrorHandler implements InboundHandler {
	private final WsClient ws;

	public ErrorHandler(WsClient ws) {
		this.ws = ws;
	}

	@Override
	public void handle(Envelope env) {
		Log.warn("Server ERROR: {}", String.valueOf(env.payload()));
	}
}
