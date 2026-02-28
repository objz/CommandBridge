package dev.objz.commandbridge.net.payloads.util;

import java.util.Set;
import java.util.UUID;

public record PlayerListPayload(Set<UUID> players) {
}
