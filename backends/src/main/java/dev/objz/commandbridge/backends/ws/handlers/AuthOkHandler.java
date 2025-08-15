package dev.objz.commandbridge.backends.ws.handlers;

import dev.objz.commandbridge.backends.ws.ClientWebSocket;
import dev.objz.commandbridge.backends.ws.IncomingDispatcher.InboundHandler;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;

public final class AuthOkHandler implements InboundHandler {
	private final ClientWebSocket ws;

	public AuthOkHandler(ClientWebSocket ws) {
		this.ws = ws;
	}

	@Override
	public void handle(Envelope env) {
		ws.markAuthenticated();
		Log.success("Authenticated.");
	}
}
