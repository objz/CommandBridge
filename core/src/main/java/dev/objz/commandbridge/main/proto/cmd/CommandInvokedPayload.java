package dev.objz.commandbridge.main.proto.cmd;

import java.util.Map;

public record CommandInvokedPayload(
        String commandId,
        String alias,
        String backendId,
        String executorName,
        String executorUuid,
        String world,
        double x, double y, double z,
        long tsMillis,
        Map<String,String> rawArgs
) {}
