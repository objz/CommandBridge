package dev.objz.commandbridge.velocity.dispatch.model;

import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import java.util.Map;
import java.util.UUID;

public record ScheduledTask(
        UUID id,
        UUID playerUuid,
        String scriptName,
        CmdMapping commandMapping,
        Map<String, Object> arguments,
        int commandIndex,
        long timestamp) {
}
