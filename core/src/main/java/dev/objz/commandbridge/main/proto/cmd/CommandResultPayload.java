package dev.objz.commandbridge.main.proto.cmd;

import java.util.UUID;

public record CommandResultPayload(
        UUID planId,            // correlate back to EXECUTE_COMMAND
        String targetBackendId,
        boolean success,
        String output,          // aggregated output or short message
        String error,           // error if any
        long durationMs
) {}
