package dev.objz.commandbridge.backends.ws.handlers;

import dev.objz.commandbridge.backends.ws.ClientWebSocket;
import dev.objz.commandbridge.backends.ws.IncomingDispatcher.InboundHandler;
import dev.objz.commandbridge.main.proto.Envelope;

public final class PingHandler implements InboundHandler {
	private final ClientWebSocket ws;

	public PingHandler(ClientWebSocket ws) {
		this.ws = ws;
	}

	@Override
	public void handle(Envelope env) {
		ws.send(Envelope.pong(env, ws.clientId()));
	}
}
