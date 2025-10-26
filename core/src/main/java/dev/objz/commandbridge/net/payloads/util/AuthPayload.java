package dev.objz.commandbridge.net.payloads.util;

public record AuthPayload(
		String clientId,
		String clientNonce,
		String hmac) {
}
