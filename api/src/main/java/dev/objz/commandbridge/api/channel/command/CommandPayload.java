package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.ChannelPayload;

import java.util.UUID;

/**
 * Payload for command execution.
 *
 * @param command the command string to run
 * @param runAs the execution mode
 * @param player the optional player context
 */
public record CommandPayload(String command, RunAs runAs, UUID player) implements ChannelPayload {

    /** Creates a payload for console execution. */
    public static CommandPayload console(String command) {
        return new CommandPayload(command, RunAs.CONSOLE, null);
    }

    /** Creates a payload for player execution. */
    public static CommandPayload player(String command, UUID player) {
        return new CommandPayload(command, RunAs.PLAYER, player);
    }

    /** Creates a payload for operator execution. */
    public static CommandPayload operator(String command, UUID player) {
        return new CommandPayload(command, RunAs.OPERATOR, player);
    }
}
