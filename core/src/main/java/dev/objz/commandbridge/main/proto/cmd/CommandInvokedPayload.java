package dev.objz.commandbridge.main.proto.cmd;

import java.util.Map;

public record CommandInvokedPayload(
        String commandId,      // script name on velocity
        String alias,          // which alias used
        String backendId,      // backend server-id that received it
        String executorName,
        String executorUuid,   // string form
        String world,
        double x, double y, double z,
        long tsMillis,
        Map<String,String> rawArgs // tokenized args for velocity to validate/resolve
) {}
