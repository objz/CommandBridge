package dev.objz.commandbridge.velocity.dispatch.model;

import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ScheduledTask(
        UUID id,
        UUID playerUuid,
        String scriptName,
        CmdMapping commandMapping,
        Map<String, Object> arguments,
        int commandIndex,
        long timestamp,
        Script authorizedScript) {

    public ScheduledTask {
        var restored = new HashMap<String, Object>(arguments == null ? Map.of() : arguments);
        if (authorizedScript != null && authorizedScript.args() != null) {
            for (var argument : authorizedScript.args()) {
                if (restored.containsKey(argument.name())) {
                    restored.put(argument.name(), InvokedCommand.normalizeValue(argument.type(), restored.get(argument.name())));
                }
            }
        }
        arguments = Collections.unmodifiableMap(restored);
    }

    public ScheduledTask(UUID id, UUID playerUuid, String scriptName, CmdMapping commandMapping,
            Map<String, Object> arguments, int commandIndex, long timestamp) {
        this(id, playerUuid, scriptName, commandMapping, arguments, commandIndex, timestamp, null);
    }
}
