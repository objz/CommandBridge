package dev.objz.commandbridge.net.payloads.util;

public record AuthRequestPayload(
		String clientNonce,
		String hmac) {
}
