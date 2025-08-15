package dev.objz.commandbridge.main.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.security.AuthService;
import dev.objz.commandbridge.main.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Set;

public final class AuthHandler implements ServerMessageHandler {
	private final SessionHub sessions;
	private final AuthService auth;
	private final String serverId;

	public AuthHandler(SessionHub sessions, AuthService auth, String serverId) {
		this.sessions = sessions;
		this.auth = auth;
		this.serverId = serverId;
	}

	@Override
	public void handle(WebSocketChannel ch, Envelope env) {
		JsonNode p = env.payload();

		String clientId = p.path("clientId").asText("");
		String secret = p.hasNonNull("secret") ? p.get("secret").asText() : null;
		String nonce = p.hasNonNull("nonce") ? p.get("nonce").asText() : null;
		String hmac = p.hasNonNull("hmac") ? p.get("hmac").asText() : null;

		boolean ok = false;
		if (secret != null && !secret.isBlank()) {
			ok = auth.verifyShared(secret);
		} else if (!clientId.isBlank() && nonce != null && hmac != null) {
			ok = auth.verify(clientId, nonce, hmac);
		}

		if (!ok) {
			Log.warn("AUTH failed for {} from {}", clientId, ch.getSourceAddress());
			try {
				ch.close();
			} catch (Exception ignored) {
			}
			sessions.remove(ch);
			return;
		}

		sessions.authed(ch, clientId, Set.of());
		sessions.send(ch, Envelope.make(MessageType.AUTH_OK, serverId, clientId, null));
	}
}
