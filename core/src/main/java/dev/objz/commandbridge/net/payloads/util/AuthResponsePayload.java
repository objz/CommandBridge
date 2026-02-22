package dev.objz.commandbridge.net.payloads.util;

public record AuthResponsePayload(
        String serverNonce,
        String hmac) {
}
