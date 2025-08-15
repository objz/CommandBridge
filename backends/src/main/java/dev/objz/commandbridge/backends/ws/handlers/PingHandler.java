package dev.objz.commandbridge.backends.ws.handlers;

import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.backends.ws.IncomingDispatcher.InboundHandler;
import dev.objz.commandbridge.main.proto.Envelope;

public final class PingHandler implements InboundHandler {
	private final WsClient ws;

	public PingHandler(WsClient ws) {
		this.ws = ws;
	}

	@Override
	public void handle(Envelope env) {
		ws.send(Envelope.pong(env, ws.clientId()));
	}
}
