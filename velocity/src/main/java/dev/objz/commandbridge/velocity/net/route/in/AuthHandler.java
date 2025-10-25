package dev.objz.commandbridge.velocity.net.route.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.route.InboundRouter;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.net.session.SessionHub.CloseReason;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class AuthHandler implements InboundRouter.InboundHandler {

	private final String serverId;
	private final AuthService auth;
	private final SessionHub sessions;
	private final ObjectMapper mapper;

	private volatile Consumer<ClientSession> onAuthed;

	public AuthHandler(String serverId, AuthService auth, SessionHub sessions, ObjectMapper mapper) {
		this.serverId = Objects.requireNonNull(serverId, "serverId");
		this.auth = Objects.requireNonNull(auth, "auth");
		this.sessions = Objects.requireNonNull(sessions, "sessions");
		this.mapper = Objects.requireNonNull(mapper, "mapper");
	}

	public void register(InboundRouter router) {
		router.register(MessageType.AUTH, this);
	}

	public void onAuthenticated(Consumer<ClientSession> listener) {
		this.onAuthed = listener;
	}

	@Override
	public void accept(WebSocketChannel ch, Envelope env) {
		if (env.type() != MessageType.AUTH || env.payload() == null) {
			close(ch, CloseReason.MISSING_AUTH);
			return;
		}

		JsonNode p = env.payload();
		String clientId = p.path("clientId").asText("");
		String cNonce = p.hasNonNull("nonce") ? p.get("nonce").asText() : null;
		String cMac = p.hasNonNull("hmac") ? p.get("hmac").asText() : null;

		boolean ok = (!clientId.isBlank() && cNonce != null && cMac != null)
				&& auth.verify(clientId, cNonce, cMac);

		if (!ok) {
			sendImmediate(ch, Envelope.make(MessageType.AUTH_FAIL, serverId, clientId, null));
			close(ch, CloseReason.AUTH_FAILED);
			Log.error("AUTH failed for '{}' from '{}'", clientId, ch.getSourceAddress());
			return;
		}

		String sNonce = UUID.randomUUID().toString().replace("-", "");
		String sMac = auth.signServerProof(clientId, cNonce, sNonce);

		ObjectNode payload = mapper.createObjectNode()
				.put("serverNonce", sNonce)
				.put("hmac", sMac);

		ClientSession s = sessions.add(ch, clientId);
		s.status(AuthStatus.AUTHENTICATED);

		sendImmediate(ch, Envelope.make(MessageType.AUTH_OK, serverId, clientId, payload));

		var cb = onAuthed;
		if (cb != null) {
			try {
				cb.accept(s);
			} catch (Exception e) {
				Log.debug("onAuthenticated listener failed: {}", e.toString());
			}
		}
	}

	private static void sendImmediate(WebSocketChannel ch, Envelope env) {
		try {
			WebSockets.sendText(new ObjectMapper().writeValueAsString(env), ch, null);
		} catch (Exception e) {
			try {
				Log.warn("WS send failed to {}: {}", ch.getSourceAddress(), e.toString());
			} catch (Exception ignored) {
			}
		}
	}

	private static void close(WebSocketChannel ch, CloseReason reason) {
		try {
			WebSockets.sendClose(reason.code, reason.reason, ch, null);
		} catch (Exception ignored) {
			try {
				ch.close();
			} catch (Exception ignored2) {
			}
		}
	}
}
