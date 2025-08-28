package dev.objz.commandbridge.main.proto.cmd;

public record CommandResultPayload(
        String targetBackendId,
        boolean success,
        String output,      // aggregated output or short message
        String error,       // error if any
        long durationMs
) {}
