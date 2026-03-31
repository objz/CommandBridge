package dev.objz.commandbridge.api.platform;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for resolving which server a player is currently connected to within the bridge network.
 *
 * <p>A {@code PlayerLocator} is available only on the Velocity proxy. Obtain an instance via
 * {@link dev.objz.commandbridge.api.CommandBridgeAPI#playerLocator()}. On backend servers,
 * that method returns {@code Optional.empty()}.
 *
 * <p>The following example locates a player and sends a command to their current server:
 *
 * <pre>{@code
 * api.playerLocator().ifPresent(locator -> {
 *     locator.locate(playerUUID).ifPresent(target -> {
 *         api.channel(CommandPayload.class)
 *            .to(List.of(target))
 *            .send(new CommandPayload("home", RunAs.PLAYER, playerUUID));
 *     });
 * });
 * }</pre>
 *
 * @see dev.objz.commandbridge.api.CommandBridgeAPI#playerLocator()
 * @see dev.objz.commandbridge.api.platform.Platform.ServerTarget
 */
@FunctionalInterface
public interface PlayerLocator {

    /**
     * Resolves the server a player is currently connected to.
     *
     * @param player the UUID of the player to locate
     * @return an {@link java.util.Optional} containing the {@link Platform.ServerTarget} of the
     *         server the player is connected to, or an empty {@code Optional} if the player is
     *         offline or their location is unknown
     * @see dev.objz.commandbridge.api.platform.Platform.ServerTarget
     */
    Optional<Platform.ServerTarget> locate(UUID player);
}
