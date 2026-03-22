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

    public CommandPayload(String command, RunAs runAs) {
        this(command, runAs, null);
    }
}
