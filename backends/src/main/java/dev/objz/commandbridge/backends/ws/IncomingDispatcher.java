package dev.objz.commandbridge.backends.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.backends.ws.handlers.ErrorHandler;
import dev.objz.commandbridge.backends.ws.handlers.PingHandler;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.backends.ws.handlers.AuthHandler;

import java.util.EnumMap;
import java.util.Map;

public final class IncomingDispatcher {
	private final Map<MessageType, InboundHandler> byType = new EnumMap<>(MessageType.class);
	private final ObjectMapper mapper;

	public interface InboundHandler {
		void handle(Envelope env) throws Exception;
	}

	public IncomingDispatcher(ClientWebSocket ws, ObjectMapper mapper) {
		this.mapper = mapper;
		byType.put(MessageType.AUTH_OK, new AuthHandler(ws, AuthHandler.AuthStatus.AUTHENTICATED));
		byType.put(MessageType.AUTH_FAIL, new AuthHandler(ws, AuthHandler.AuthStatus.NOT_AUTHENTICATED));
		byType.put(MessageType.PING, new PingHandler(ws));
		byType.put(MessageType.ERROR, new ErrorHandler(ws));
	}

	public void dispatch(String json) {
		Envelope env;
		try {
			env = mapper.readValue(json, Envelope.class);
		} catch (Exception e) {
			Log.warn("Bad WS message: {}", e.getMessage());
			return;
		}

		InboundHandler h = byType.get(env.type());
		if (h == null) {
			if (Log.isDebug())
				Log.debug("Unhandled WS message: {}", json);
			return;
		}
		try {
			h.handle(env);
		} catch (Exception ex) {
			Log.error(ex, "Inbound handler failed for {}", env.type());
		}
	}
}
