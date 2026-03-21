package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.ChannelPayload;

import java.util.UUID;

public record CommandPayload(String command, RunAs runAs, UUID player) implements ChannelPayload {

    public static CommandPayload console(String command) {
        return new CommandPayload(command, RunAs.CONSOLE, null);
    }

    public static CommandPayload player(String command, UUID player) {
        return new CommandPayload(command, RunAs.PLAYER, player);
    }

    public static CommandPayload operator(String command, UUID player) {
        return new CommandPayload(command, RunAs.OPERATOR, player);
    }
}
