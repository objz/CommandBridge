package dev.objz.commandbridge.velocity.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.net.InboundRouter;
import dev.objz.commandbridge.net.payloads.util.AuthPayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class AuthHandler implements InboundRouter.InboundHandler {

	private final AuthService auth;
	private final SessionHub sessions;
	private final WsServer ws;

	private volatile Consumer<ClientSession> onAuthed;

	public AuthHandler(AuthService auth, SessionHub sessions, WsServer ws) {
		this.auth = Objects.requireNonNull(auth);
		this.sessions = Objects.requireNonNull(sessions);
		this.ws = Objects.requireNonNull(ws);
	}

	public void register(InboundRouter router) {
		router.register(MessageType.AUTH_REQUEST, this);
	}

	public void onAuthenticated(Consumer<ClientSession> listener) {
		this.onAuthed = listener;
	}

	@Override
	public void accept(WebSocketChannel ch, Envelope env) {
		if (env.type() != MessageType.AUTH_REQUEST || env.payload() == null) {
			ws.close(ch);
			return;
		}

		AuthPayload ap = null;
		try {
			ap = Envelope.MAPPER.treeToValue(env.payload(), AuthPayload.class);
		} catch (Exception e) {
			Log.error(e, "Failed to handle AUTH_REQUEST from {}", env.from());
		}

		if (ap == null || ap.clientId() == null || ap.clientNonce() == null || ap.hmac() == null) {
			reply(ch, env, null, MessageType.AUTH_FAIL);
			ws.close(ch);
			Log.error("Authentication failed (malformed payload) from '{}'", ch.getSourceAddress());
			return;
		}

		if (!auth.verify(ap.clientId(), ap.clientNonce(), ap.hmac())) {
			reply(ch, env, null, MessageType.AUTH_FAIL);
			ws.close(ch);
			Log.error("Authentication failed for '{}' from '{}'", ap.clientId(), ch.getSourceAddress());
			return;
		}

		String sNonce = UUID.randomUUID().toString().replace("-", "");
		String sMac = auth.signServerProof(ap.clientId(), ap.clientNonce(), sNonce);

		ClientSession s = sessions.add(ch, ap.clientId());
		s.status(AuthStatus.AUTH_FAIL);

		reply(ch, env, new AuthPayload(ap.clientId(), sNonce, sMac), MessageType.AUTH_OK);
		s.status(AuthStatus.AUTH_OK);
		Log.success(true, "Authentication succeeded for '{}' from '{}'", ap.clientId(), ch.getSourceAddress());

		var cb = onAuthed;
		if (cb != null) {
			try {
				cb.accept(s);
			} catch (Exception e) {
				Log.error("Authentication listener failed: {}", e.toString());
			}
		}
	}

	private void reply(WebSocketChannel ch, Envelope req, AuthPayload p, MessageType type) {
		var payload = Envelope.MAPPER.valueToTree(p);
		Envelope resp = Envelope.reply(req, type, req.to(), payload);
		try {
			ws.send(ch, resp)
					.dispatch()
					.exceptionally(ex -> {
						Log.warn("Failed to send auth response: {}", ex.toString());
						return null;
					});
		} catch (Exception e) {
			Log.warn("Failed to send auth response: {}", e.toString());
		}
	}

}
