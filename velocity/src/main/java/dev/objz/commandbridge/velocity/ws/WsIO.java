package dev.objz.commandbridge.velocity.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.WebSocketChannel;

final class WsIO {
	private static final ObjectMapper M = new ObjectMapper();

	private WsIO() {
	}

	static void sendText(WebSocketChannel ch, Envelope env) {
		try {
			String json = M.writeValueAsString(env);
			WebSockets.sendText(json, ch, null); 
		} catch (Exception e) {
			Log.warn("WS send failed to {}: {}", ch.getSourceAddress(), e.toString());
		}
	}
}
