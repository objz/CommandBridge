package dev.objz.commandbridge.velocity.ws.handlers;

import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.velocity.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.velocity.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

public final class PingHandler implements InboundHandler {
	private final SessionHub sessions;
	private final String serverId;

	public PingHandler(SessionHub sessions, String serverId) {
		this.sessions = sessions;
		this.serverId = serverId;
	}

	@Override
	public void handle(WebSocketChannel ch, Envelope env) {
		sessions.send(ch, Envelope.pong(env, serverId));
	}
}
