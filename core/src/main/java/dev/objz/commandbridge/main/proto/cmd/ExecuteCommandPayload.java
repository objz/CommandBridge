package dev.objz.commandbridge.main.proto.cmd;

public record ExecuteCommandPayload(
        String targetBackendId,
        String asMode,   // PLAYER | CONSOLE | OP_PLAYER (string of ScriptTypes.ExecutorMode)
        String runAs,    // player name or null if console
        String command,  // fully rendered command
        long timeoutMs
) {}
