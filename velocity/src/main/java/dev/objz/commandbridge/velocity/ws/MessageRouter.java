package dev.objz.commandbridge.velocity.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.security.AuthService;
import dev.objz.commandbridge.main.security.AuthStatus;
import dev.objz.commandbridge.velocity.ws.handlers.AuthHandler;
import dev.objz.commandbridge.velocity.ws.handlers.FeedbackHandler;
import dev.objz.commandbridge.velocity.ws.handlers.PingHandler;
import dev.objz.commandbridge.velocity.ws.handlers.PongHandler;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.EnumMap;
import java.util.Map;

public final class MessageRouter {
	private final ObjectMapper mapper;
	private final Map<MessageType, InboundHandler> handlers;
	private final SessionHub sessions;

	public interface InboundHandler {
		void handle(WebSocketChannel ch, Envelope env) throws Exception;
	}

	public MessageRouter(ObjectMapper mapper, SessionHub sessions, AuthService auth, String serverId) {
		this.mapper = mapper;
		this.sessions = sessions;
		this.handlers = new EnumMap<>(MessageType.class);

		handlers.put(MessageType.AUTH, new AuthHandler(sessions, auth, serverId));
		handlers.put(MessageType.PING, new PingHandler(sessions, serverId));
		handlers.put(MessageType.PONG, new PongHandler(sessions));
		handlers.put(MessageType.FEEDBACK, new FeedbackHandler(mapper, sessions));
	}

	public void register(MessageType type, InboundHandler handler) {
		handlers.put(type, handler);
	}

	public void onText(WebSocketChannel ch, String text) {
		Envelope env;
		try {
			env = mapper.readValue(text, Envelope.class);
		} catch (Exception e) {
			Log.warn("Bad JSON from {}: {}", ch.getSourceAddress(), e.getMessage());
			return;
		}

		ClientSession s = sessions.all().stream().filter(cs -> cs.ch() == ch).findFirst().orElse(null);
		if (s != null && s.status() != AuthStatus.AUTHENTICATED && env.type() != MessageType.AUTH) {
			Log.warn("Dropping {} from unauthenticated client {}", env.type(), ch.getSourceAddress());
			return;
		}

		InboundHandler h = handlers.get(env.type());
		if (h == null) {
			Log.debug("Unhandled message type: {}", env.type());
			return;
		}
		try {
			h.handle(ch, env);
		} catch (Exception ex) {
			Log.error(ex, "Handler failure for type {}", env.type());
		}
	}
}
