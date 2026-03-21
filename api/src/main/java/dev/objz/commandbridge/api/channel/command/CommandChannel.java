package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.platform.Platform;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CommandChannel extends MessageChannel<CommandPayload> {

    default CompletableFuture<Void> console(Platform.ServerTarget target, String command) {
        return send(target, CommandPayload.console(command));
    }

    default CompletableFuture<Void> player(Platform.ServerTarget target, String command, UUID player) {
        return send(target, CommandPayload.player(command, player));
    }

    default CompletableFuture<Void> operator(Platform.ServerTarget target, String command, UUID player) {
        return send(target, CommandPayload.operator(command, player));
    }
}
