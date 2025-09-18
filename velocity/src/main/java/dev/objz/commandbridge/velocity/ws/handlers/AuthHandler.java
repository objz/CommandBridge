package dev.objz.commandbridge.velocity.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.security.AuthService;
import dev.objz.commandbridge.velocity.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.velocity.ws.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Set;
import java.util.UUID;

public final class AuthHandler implements InboundHandler {
	private final SessionHub sessions;
	private final AuthService auth;
	private final String serverId;
	private boolean requireAuth;
	private final ObjectMapper mapper = new ObjectMapper();

	public AuthHandler(SessionHub sessions, AuthService auth, String serverId, boolean requireAuth) {
		this.sessions = sessions;
		this.auth = auth;
		this.serverId = serverId;
		this.requireAuth = requireAuth;
	}

	@Override
	public void handle(WebSocketChannel ch, Envelope env) {
		JsonNode p = env.payload();

		String clientId = p.path("clientId").asText("");
		String cNonce = p.hasNonNull("nonce") ? p.get("nonce").asText() : null;
		String cMac = p.hasNonNull("hmac") ? p.get("hmac").asText() : null;

		boolean ok = (!clientId.isBlank() && cNonce != null && cMac != null)
				&& auth.verify(clientId, cNonce, cMac);

		if (requireAuth && !ok) {
			Log.error("AUTH failed for {} from {}", clientId, ch.getSourceAddress());
			try {
				sessions.send(ch, Envelope.make(MessageType.AUTH_FAIL, serverId, clientId, null));
				ch.close();
			} catch (Exception ignored) {
			}
			sessions.remove(ch);
			return;
		}

		String sNonce = UUID.randomUUID().toString().replace("-", "");
		String sMac = auth.signServerProof(clientId, cNonce, sNonce);

		var payload = mapper.createObjectNode()
				.put("serverNonce", sNonce)
				.put("hmac", sMac);

		sessions.send(ch, Envelope.make(MessageType.AUTH_OK, serverId, clientId, payload));
		sessions.authed(ch, clientId, Set.of());
	}
}
