package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.platform.Platform;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Specialized channel for dispatching commands to servers. */
public interface CommandChannel extends MessageChannel<CommandPayload> {

    /**
     * Executes a command as the console.
     *
     * @param target the destination server
     * @param command the command string to run
     * @return a future that completes when the command is sent
     */
    default CompletableFuture<Void> console(Platform.ServerTarget target, String command) {
        return send(target, CommandPayload.console(command));
    }

    /**
     * Executes a command as a specific player.
     *
     * @param target the destination server
     * @param command the command string to run
     * @param player the UUID of the player to run as
     * @return a future that completes when the command is sent
     */
    default CompletableFuture<Void> player(Platform.ServerTarget target, String command, UUID player) {
        return send(target, CommandPayload.player(command, player));
    }

    /**
     * Executes a command with operator permissions, bypassing standard checks.
     *
     * @param target the destination server
     * @param command the command string to run
     * @param player the UUID of the player to run as
     * @return a future that completes when the command is sent
     */
    default CompletableFuture<Void> operator(Platform.ServerTarget target, String command, UUID player) {
        return send(target, CommandPayload.operator(command, player));
    }
}
