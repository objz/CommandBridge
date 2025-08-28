package dev.objz.commandbridge.main.proto.cmd;

import java.util.UUID;

public record ExecuteCommandPayload(
        UUID planId,            // correlation id (envelope id mirror is okay; explicit is safer)
        String targetBackendId,
        String asMode,          // PLAYER | CONSOLE | OP_PLAYER
        String runAs,           // player name or null if console
        String command,         // fully rendered command
        long timeoutMs
) {}
