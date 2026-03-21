package dev.objz.commandbridge.api.platform;

import java.util.Optional;
import java.util.UUID;

/** Service for finding which server a player is currently connected to. */
@FunctionalInterface
public interface PlayerLocator {
    /**
     * Resolves the server target for a player UUID.
     *
     * @param player the UUID of the player to locate
     * @return the server target, or empty if the player is offline or not found
     */
    Optional<Platform.ServerTarget> locate(UUID player);
}
