package dev.objz.commandbridge.backends.net.out.ctx;

import java.util.Set;
import java.util.UUID;

public record PlayerListContext(Set<UUID> players) {
}
