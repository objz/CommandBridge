package dev.objz.commandbridge.backends.ws.handlers;

import dev.objz.commandbridge.backends.ws.ClientWebSocket;
import dev.objz.commandbridge.backends.ws.IncomingDispatcher.InboundHandler;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;

public final class AuthHandler implements InboundHandler {
	private final ClientWebSocket ws;
	private final AuthStatus status;

	public enum AuthStatus {
		NOT_AUTHENTICATED,
		AUTHENTICATED
	}

	public AuthHandler(ClientWebSocket ws, AuthStatus status) {
		this.ws = ws;
		this.status = status;
	}

	@Override
	public void handle(Envelope env) {
		if (status == AuthStatus.AUTHENTICATED) {
			ws.markAuthenticated();
			Log.success("Authenticated");
		} else {
			ws.markNotAuthenticated(); 
			Log.error("Authentication failed");
			ws.close(); 
		}
	}
}
