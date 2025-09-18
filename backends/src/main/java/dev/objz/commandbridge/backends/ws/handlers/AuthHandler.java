package dev.objz.commandbridge.backends.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.backends.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.config.model.TlsMode;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.security.AuthStatus;

public final class AuthHandler implements InboundHandler {
	private final WsClient ws;
	private final AuthStatus status;
	private final BackendsConfig config;

	public AuthHandler(WsClient ws, AuthStatus status, BackendsConfig config) {
		this.ws = ws;
		this.status = status;
		this.config = config;
	}

	@Override
	public void handle(Envelope env) {
		if (status == AuthStatus.AUTHENTICATED) {
			if (!ws.requireAuth() && config.security().tlsMode() == TlsMode.PLAIN) {
				ws.markAuthenticated();
				ws.persistTlsPinIfNeeded();
				Log.success("Authenticated to unverified server");
				return;
			}

			JsonNode p = env.payload();
			String sNonce = p.hasNonNull("serverNonce") ? p.get("serverNonce").asText() : null;
			String sMac = p.hasNonNull("hmac") ? p.get("hmac").asText() : null;
			boolean ok = (sNonce != null && sMac != null)
					&& ws.auth().verifyServerProof(ws.clientId(), ws.getClientNonce(), sNonce,
							sMac);
			if (!ok) {
				Log.error("Authentication failed: invalid server proof");
				ws.markNotAuthenticated();
				ws.close();
				return;
			}
			ws.markAuthenticated();
			ws.persistTlsPinIfNeeded();
			Log.success("Authenticated");
		} else {
			ws.markNotAuthenticated();
			Log.error("Authentication failed");
			ws.close();
		}
	}
}
