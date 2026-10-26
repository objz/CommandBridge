package dev.objz.commandbridge.net.payloads.util;

import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.enums.ServerPlatform;

public record AuthRequestPayload(
        Location location,
        ServerPlatform platform,
        String clientNonce,
        String hmac) {
}
