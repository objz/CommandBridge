package dev.objz.commandbridge.backends.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.backends.PlatformInterface;
import dev.objz.commandbridge.backends.ws.handlers.AuthHandler;
import dev.objz.commandbridge.backends.ws.handlers.ErrorHandler;
import dev.objz.commandbridge.backends.ws.handlers.PingHandler;
import dev.objz.commandbridge.backends.ws.handlers.RegisterCommandsHandler;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.proto.PreAuth;
import dev.objz.commandbridge.main.security.AuthStatus;
import dev.objz.commandbridge.main.util.RateLimiter;

import java.util.EnumMap;
import java.util.Map;

public final class MessageRouter {
	private final Map<MessageType, InboundHandler> byType = new EnumMap<>(MessageType.class);
	private final ObjectMapper mapper;
	private final WsClient ws;
	private final RateLimiter<String> limiter = new RateLimiter<>(60); // single-conn limiter

	public interface InboundHandler {
		void handle(Envelope env) throws Exception;
	}

	public MessageRouter(WsClient ws, ObjectMapper mapper, PlatformInterface platform, BackendsConfig config) {
		this.ws = ws;
		this.mapper = mapper;
		byType.put(MessageType.AUTH_OK,
				new AuthHandler(ws, AuthStatus.AUTHENTICATED, config));
		byType.put(MessageType.AUTH_FAIL,
				new AuthHandler(ws, AuthStatus.NOT_AUTHENTICATED, config));
		byType.put(MessageType.PING, new PingHandler(ws));
		byType.put(MessageType.ERROR, new ErrorHandler(ws));
		byType.put(MessageType.REGISTER_COMMANDS, new RegisterCommandsHandler(ws, mapper, platform));
	}

	public void dispatch(String json) {
		if (!limiter.allow("inbound")) {
			Log.warn("Rate limit exceeded from server");
			return;
		}
		final Envelope env;
		try {
			env = mapper.readValue(json, Envelope.class);
		} catch (Exception e) {
			Log.warn("Bad WS message: {}", e.getMessage());
			return;
		}

		boolean authed = (ws.state() == ClientState.AUTHENTICATED);
		if (!PreAuth.backendInboundAllowed(authed, env.type())) {
			Log.warn("Dropping {} before AUTH_OK", env.type());
			return;
		}

		final InboundHandler h = byType.get(env.type());
		if (h == null) {
			Log.debug("Unhandled WS message: {}", env.type());
			return;
		}

		try {
			h.handle(env);
		} catch (Exception ex) {
			Log.error(ex, "Inbound handler failed for {}", env.type());
		}
	}
}
