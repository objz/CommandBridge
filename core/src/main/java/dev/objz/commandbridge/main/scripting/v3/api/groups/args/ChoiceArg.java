package dev.objz.commandbridge.main.scripting.v3.api.groups.args;

import java.util.List;
import java.util.Set;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.Arg;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;

public record ChoiceArg(
		String name,
		boolean required,
		Set<TargetKind.Type> allowedRegisters,
		List<String> choices) implements Arg {

	@Override
	public ArgType type() {
		return ArgType.CHOICE;
	}

	@Override
	public void validate(ProblemSink problems) {
		Arg.super.validate(problems);
		if (choices.isEmpty()) {
			problems.error("args[" + name() + "].choices", "Choices cannot be empty");
		}
	}

	@Override
	public Object coerceRuntimeValue(Object raw, ProblemSink problems) {
		String value = switch (raw) {
			case String s -> s;
			case null -> required ? null : choices.get(0);
			default -> raw.toString();
		};

		if (value != null && !choices.contains(value)) {
			problems.error("args[" + name() + "].value",
					"Value '" + value + "' is not in allowed choices: " + choices);
			return choices.get(0); // return first choice 
		}

		return value;
	}

	public static ChoiceArg withDefaults(String name, boolean required, List<String> choices) {
		return new ChoiceArg(name, required, ArgType.CHOICE.getDefaultAllowedRegisters(), choices);
	}

	public static ChoiceArg backendOnly(String name, boolean required, List<String> choices) {
		return new ChoiceArg(name, required, Set.of(TargetKind.Type.BACKEND), choices);
	}

	public static ChoiceArg both(String name, boolean required, List<String> choices) {
		return new ChoiceArg(name, required, Set.of(TargetKind.Type.BACKEND, TargetKind.Type.VELOCITY),
				choices);
	}
}
