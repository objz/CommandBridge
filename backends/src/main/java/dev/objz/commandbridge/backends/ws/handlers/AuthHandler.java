package dev.objz.commandbridge.backends.ws.handlers;

import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.backends.ws.MessageRouter.InboundHandler;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.security.AuthStatus;

public final class AuthHandler implements InboundHandler {
	private final WsClient ws;
	private final AuthStatus status;


	public AuthHandler(WsClient ws, AuthStatus status) {
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
