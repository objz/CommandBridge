package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import java.util.Map;

public record CommandOverride(Map<String, PacketArgumentSpec> argumentSpecs) {
    public CommandOverride {
        argumentSpecs = Map.copyOf(argumentSpecs);
    }

    public PacketArgumentSpec argumentSpec(String argumentName) {
        return argumentSpecs.get(argumentName);
    }

    public boolean isEmpty() {
        return argumentSpecs.isEmpty();
    }
}
