package dev.objz.commandbridge.net.payloads.cmd;

import java.util.List;
import java.util.UUID;

public record ExecuteCommandResult(
        boolean success,
        String command,
        UUID playerUuid,
        String message,
        List<String> errors) {

    public static ExecuteCommandResult success(String command, UUID playerUuid) {
        return new ExecuteCommandResult(true, command, playerUuid, null, List.of());
    }

    public static ExecuteCommandResult failure(String command, UUID playerUuid, String message) {
        return new ExecuteCommandResult(false, command, playerUuid, message, List.of(message));
    }

    public static ExecuteCommandResult failure(String command, UUID playerUuid, String message,
            List<String> errors) {
        return new ExecuteCommandResult(false, command, playerUuid, message, errors);
    }
}
