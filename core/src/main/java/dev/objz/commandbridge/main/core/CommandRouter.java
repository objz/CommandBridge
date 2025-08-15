package dev.objz.commandbridge.main.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.security.AuthService;
import dev.objz.commandbridge.main.ws.SessionHub;
import dev.objz.commandbridge.main.ws.handlers.AuthHandler;
import dev.objz.commandbridge.main.ws.handlers.PingHandler;
import dev.objz.commandbridge.main.ws.handlers.PongHandler;
import dev.objz.commandbridge.main.ws.handlers.ServerMessageHandler;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.EnumMap;
import java.util.Map;

public final class CommandRouter {
	private final ObjectMapper mapper;
	private final Map<MessageType, ServerMessageHandler> handlers;

	public CommandRouter(ObjectMapper mapper, SessionHub sessions, AuthService auth, String serverId) {
		this.mapper = mapper;
		this.handlers = new EnumMap<>(MessageType.class);

		handlers.put(MessageType.AUTH, new AuthHandler(sessions, auth, serverId));
		handlers.put(MessageType.PING, new PingHandler(sessions, serverId));
		handlers.put(MessageType.PONG, new PongHandler(sessions));
	}

	public void onText(WebSocketChannel ch, String text) {
		Envelope env;
		try {
			env = mapper.readValue(text, Envelope.class);
		} catch (Exception e) {
			Log.warn("Bad JSON from {}: {}", ch.getSourceAddress(), e.getMessage());
			return;
		}

		ServerMessageHandler h = handlers.get(env.type());
		if (h == null) {
			if (Log.isDebug())
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
