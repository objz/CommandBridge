package dev.objz.commandbridge.velocity;

import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.List;

public final class StubExporter {

	private StubExporter() {
	}

	public static CommandStub export(Script script) {
		if (script == null) {
			throw new IllegalArgumentException("Script cannot be null");
		}

		String name = script.name();
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Script name cannot be null or blank");
		}

		List<String> aliases = script.aliases() != null ? script.aliases() : List.of();
		String description = script.description();

		List<ArgMapping> usedArgs = script.usedArguments();

		return new CommandStub(name, aliases, description, usedArgs);
	}
}
