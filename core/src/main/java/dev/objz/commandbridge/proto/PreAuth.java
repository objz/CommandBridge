package dev.objz.commandbridge.proto;

import java.util.EnumSet;

public final class PreAuth {
	private PreAuth() {
	}

	/** Proxy (Velocity) inbound: before auth only AUTH may pass. */
	public static final EnumSet<MessageType> PROXY_ALLOWED = EnumSet.of(MessageType.AUTH);

	/**
	 * Proxy (Velocity) outbound while unauthenticated: only handshake + keepalive.
	 */
	public static final EnumSet<MessageType> PROXY_OUTBOUND_ALLOWED = EnumSet.of(MessageType.AUTH,
			MessageType.AUTH_OK, MessageType.AUTH_FAIL, MessageType.PING, MessageType.PONG);

	/** Backend inbound: before auth only AUTH_OK / AUTH_FAIL may pass. */
	public static final EnumSet<MessageType> CLIENT_ALLOWED = EnumSet.of(MessageType.AUTH_OK,
			MessageType.AUTH_FAIL);

	// Small helpers so both sides call the same logic
	public static boolean proxyInboundAllowed(boolean authed, MessageType t) {
		return authed || PROXY_ALLOWED.contains(t);
	}

	public static boolean proxyOutboundAllowed(boolean authed, MessageType t) {
		return authed || PROXY_OUTBOUND_ALLOWED.contains(t);
	}

	public static boolean backendInboundAllowed(boolean authed, MessageType t) {
		return authed || CLIENT_ALLOWED.contains(t);
	}
}
