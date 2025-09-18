
package dev.objz.commandbridge.velocity.ws;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.PreAuth;
import dev.objz.commandbridge.main.security.AuthService;
import dev.objz.commandbridge.main.security.AuthStatus;
import dev.objz.commandbridge.main.util.RateLimiter;
import dev.objz.commandbridge.velocity.ws.handlers.AuthHandler;
import dev.objz.commandbridge.velocity.ws.handlers.FeedbackHandler;
import dev.objz.commandbridge.velocity.ws.handlers.PingHandler;
import dev.objz.commandbridge.velocity.ws.handlers.PongHandler;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.EnumMap;
import java.util.Map;

public final class MessageRouter {
	private final ObjectMapper mapper;
	private final Map<MessageType, InboundHandler> handlers = new EnumMap<>(MessageType.class);
	private final SessionHub sessions;
	private final boolean requireAuth;

	private final RateLimiter<WebSocketChannel> limiter;

	public interface InboundHandler {
		void handle(WebSocketChannel ch, Envelope env) throws Exception;
	}

	public MessageRouter(ObjectMapper mapper, SessionHub sessions, AuthService auth, String serverId,
			boolean requireAuth, VelocityConfig config) {
		this.mapper = mapper;
		this.sessions = sessions;
		this.requireAuth = requireAuth;
		this.limiter = new RateLimiter<>(config.limits().inboundMessagesSec());
		handlers.put(MessageType.AUTH, new AuthHandler(sessions, auth, serverId, requireAuth));
		handlers.put(MessageType.PING, new PingHandler(sessions, serverId));
		handlers.put(MessageType.PONG, new PongHandler(sessions));
		handlers.put(MessageType.FEEDBACK, new FeedbackHandler(mapper, sessions));
	}

	public void register(MessageType type, InboundHandler handler) {
		handlers.put(type, handler);
	}

	public void onText(WebSocketChannel ch, String text) {
		if (!limiter.allow(ch)) {
			Log.warn("Rate limit exceeded from {}", ch.getSourceAddress());
			return;
		}

		final Envelope env;
		try {
			env = mapper.readValue(text, Envelope.class);
		} catch (Exception e) {
			Log.warn("Bad JSON from {}: {}", ch.getSourceAddress(), e.getMessage());
			return;
		}

		final ClientSession s = sessions.find(ch);
		final boolean authed = (s != null && s.status() == AuthStatus.AUTHENTICATED);
		if (requireAuth) {
			if (!PreAuth.proxyInboundAllowed(authed, env.type())) {
				Log.warn("Dropping {} from unauthenticated client {}", env.type(),
						ch.getSourceAddress());
				return;
			}
		}

		final InboundHandler h = handlers.get(env.type());
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
