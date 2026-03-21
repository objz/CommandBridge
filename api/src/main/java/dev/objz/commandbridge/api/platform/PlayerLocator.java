package dev.objz.commandbridge.api.platform;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface PlayerLocator {
    Optional<Platform.ServerTarget> locate(UUID player);
}
