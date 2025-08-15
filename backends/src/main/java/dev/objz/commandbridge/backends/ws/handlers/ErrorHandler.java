package dev.objz.commandbridge.backends.ws.handlers;

import dev.objz.commandbridge.backends.ws.ClientWebSocket;
import dev.objz.commandbridge.backends.ws.IncomingDispatcher.InboundHandler;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;

public final class ErrorHandler implements InboundHandler {
	private final ClientWebSocket ws;

	public ErrorHandler(ClientWebSocket ws) {
		this.ws = ws;
	}

	@Override
	public void handle(Envelope env) {
		Log.warn("Server ERROR: {}", String.valueOf(env.payload()));
	}
}
