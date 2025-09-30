package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import dev.objz.commandbridge.main.scripting.v3.api.Meta;
import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;

public record CommandStep(
		Meta<String> command,
		CommandOverrides overrides) {
	public void validate(ProblemSink problems, int index) {
		if (command == null || !command.hasValue() || command.effectiveValue() == null
				|| command.effectiveValue().isBlank()) {
			problems.error("commands[" + index + "].command", "Command is required and cannot be empty");
		}
		if (overrides != null) {
			overrides.validate(problems, index);
		}
	}
}
