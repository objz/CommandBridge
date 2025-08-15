package dev.objz.commandbridge.main.ws.handlers;

import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

public final class PongHandler implements ServerMessageHandler {
	private final SessionHub sessions;

	public PongHandler(SessionHub sessions) {
		this.sessions = sessions;
	}

	@Override
	public void handle(WebSocketChannel ch, Envelope env) {
		sessions.all().stream().filter(s -> s.ch() == ch).findFirst().ifPresent(s -> s.touchPong());
	}
}
