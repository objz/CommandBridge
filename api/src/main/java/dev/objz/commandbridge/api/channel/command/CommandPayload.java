package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.ChannelPayload;

import java.util.UUID;

/**
 * Payload carrying a command to be executed on a remote server via the
 * CommandBridge network.
 *
 * <p>
 * The three components define what to run and how. {@code command} is the
 * command string
 * without a leading slash. {@code runAs} defines the execution context; see
 * {@link RunAs} for
 * available modes. {@code player} is the optional player UUID required when
 * {@code runAs} is
 * {@link RunAs#PLAYER} or {@link RunAs#OPERATOR}; pass {@code null} for
 * {@link RunAs#CONSOLE}.
 *
 * <p>
 * Instances are sent through a
 * {@link dev.objz.commandbridge.api.channel.MessageChannel} obtained from
 * {@link dev.objz.commandbridge.api.CommandBridgeAPI#channel(Class)}.
 *
 * <p>
 * To run a command as the server console on a specific backend server:
 *
 * <pre>{@code
 * MessageChannel<CommandPayload> channel = api.channel(CommandPayload.class);
 * channel.to(List.of(Platform.backend("survival-1")))
 *         .send(new CommandPayload("say hello", RunAs.CONSOLE));
 * }</pre>
 *
 * <p>
 * To run a command as a specific online player with their normal permissions:
 *
 * <pre>{@code
 * channel.to(List.of(Platform.backend("survival-1")))
 *         .send(new CommandPayload("home", RunAs.PLAYER, playerUUID));
 * }</pre>
 *
 * <p>
 * To run a command as a player with temporary operator-level permissions:
 *
 * <pre>{@code
 * channel.to(List.of(Platform.backend("survival-1")))
 *         .send(new CommandPayload("gamemode creative", RunAs.OPERATOR, playerUUID));
 * }</pre>
 *
 * @param command the command string to execute on the target server; must not
 *                include a leading
 *                slash. Must not be {@code null}.
 * @param runAs   the execution context defining which identity and permissions
 *                are used; must not
 *                be {@code null}. See {@link RunAs} for available modes.
 * @param player  the UUID of the player to execute the command as, or
 *                {@code null} when
 *                {@code runAs} is {@link RunAs#CONSOLE}. Required when
 *                {@code runAs} is
 *                {@link RunAs#PLAYER} or {@link RunAs#OPERATOR}.
 * @see RunAs
 * @see dev.objz.commandbridge.api.channel.MessageChannel
 */
public record CommandPayload(String command, RunAs runAs, UUID player) implements ChannelPayload {

    /**
     * Creates a payload with no player context, equivalent to
     * {@code new CommandPayload(command, runAs, null)}.
     *
     * <p>
     * Use this constructor when {@code runAs} is {@link RunAs#CONSOLE}, as no
     * player UUID
     * is required.
     *
     * @param command the command string to execute; must not include a leading
     *                slash
     * @param runAs   the execution context; must not be {@code null}
     */
    public CommandPayload(String command, RunAs runAs) {
        this(command, runAs, null);
    }
}
