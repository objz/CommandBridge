package dev.objz.commandbridge.velocity.ws.handlers;

import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.velocity.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.velocity.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

public final class PongHandler implements InboundHandler {
	private final SessionHub sessions;

	public PongHandler(SessionHub sessions) {
		this.sessions = sessions;
	}

	@Override
	public void handle(WebSocketChannel ch, Envelope env) {
		sessions.all().stream().filter(s -> s.ch() == ch).findFirst().ifPresent(s -> s.touchPong());
	}
}
