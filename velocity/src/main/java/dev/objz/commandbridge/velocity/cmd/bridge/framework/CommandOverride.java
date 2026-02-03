package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import java.util.Map;

record CommandOverride(Map<String, PacketArgumentSpec> argumentSpecs) {
	CommandOverride {
		argumentSpecs = Map.copyOf(argumentSpecs);
	}

	PacketArgumentSpec argumentSpec(String argumentName) {
		return argumentSpecs.get(argumentName);
	}

	boolean isEmpty() {
		return argumentSpecs.isEmpty();
	}
}
