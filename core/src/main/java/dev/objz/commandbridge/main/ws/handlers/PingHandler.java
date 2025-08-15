package dev.objz.commandbridge.main.ws.handlers;

import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

public final class PingHandler implements ServerMessageHandler {
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
