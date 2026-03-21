package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.platform.Platform;

import java.util.UUID;

public interface CommandChannel extends MessageChannel<CommandPayload> {

    default void console(Platform.ServerTarget target, String command) {
        send(target, CommandPayload.console(command));
    }

    default void player(Platform.ServerTarget target, String command, UUID player) {
        send(target, CommandPayload.player(command, player));
    }

    default void operator(Platform.ServerTarget target, String command, UUID player) {
        send(target, CommandPayload.operator(command, player));
    }
}
