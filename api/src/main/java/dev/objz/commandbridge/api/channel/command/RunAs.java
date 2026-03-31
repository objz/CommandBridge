package dev.objz.commandbridge.api.channel.command;

/**
 * Defines the execution context for a command dispatched through the
 * CommandBridge network.
 *
 * <p>
 * Each constant controls which identity the backend server uses when running
 * the command.
 * The three modes cover the full range of execution contexts: server-side
 * administrative
 * commands, user-context commands with normal permissions, and elevated
 * commands that
 * temporarily grant operator status to a specific player.
 *
 * <p>
 * To run a command as the server console, with full permissions and no player
 * context:
 *
 * <pre>{@code
 * new CommandPayload("say hello", RunAs.CONSOLE)
 * }</pre>
 *
 * <p>
 * To run a command as a specific online player, with their normal permissions:
 *
 * <pre>{@code
 * new CommandPayload("home", RunAs.PLAYER, playerUUID)
 * }</pre>
 *
 * <p>
 * To run a command as a player with temporary operator-level permissions:
 *
 * <pre>{@code
 * new CommandPayload("gamemode creative", RunAs.OPERATOR, playerUUID)
 * }</pre>
 *
 * @see dev.objz.commandbridge.api.channel.command.CommandPayload
 */
public enum RunAs {

    /**
     * Executes the command as the server console.
     *
     * <p>
     * The command runs with full server-side permissions, unrestricted by player
     * permission
     * nodes. No player context is required; the {@code player} field of
     * {@link dev.objz.commandbridge.api.channel.command.CommandPayload} may be
     * {@code null}.
     */
    CONSOLE,

    /**
     * Executes the command as a specific player with their normal permissions.
     *
     * <p>
     * The command runs with the target player's permission set. The {@code player}
     * field of
     * {@link dev.objz.commandbridge.api.channel.command.CommandPayload} must be set
     * to the UUID
     * of an online player. If the player is not online, execution may be queued
     * depending on
     * bridge configuration.
     */
    PLAYER,

    /**
     * Executes the command as a specific player with temporary operator-level
     * permissions.
     *
     * <p>
     * Operator status is granted to the player for the duration of the command and
     * revoked
     * immediately after. The {@code player} field of
     * {@link dev.objz.commandbridge.api.channel.command.CommandPayload} must be
     * set. Use this
     * mode only when elevated permissions are required; prefer {@link #PLAYER} for
     * normal
     * user commands.
     */
    OPERATOR
}
