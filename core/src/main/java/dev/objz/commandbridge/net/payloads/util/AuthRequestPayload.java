package dev.objz.commandbridge.net.payloads.util;

import dev.objz.commandbridge.scripting.model.enums.Location;

public record AuthRequestPayload(
        Location location,
        String clientNonce,
        String hmac) {
}
