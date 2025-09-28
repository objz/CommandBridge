package dev.objz.commandbridge.main.scripting.v3.api.groups;

import java.util.Set;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.args.ArgType;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;

public interface Arg {

	String name();

	boolean required();

	ArgType type();

	Set<TargetKind.Type> allowedRegisters();

	default void validate(ProblemSink problems) {
		if (name() == null || name().isBlank()) {
			problems.error("args[].name", "Argument name cannot be empty");
		}
	}

	default Object coerceRuntimeValue(Object raw, ProblemSink problems) {
		return type().coerceValue(raw, problems, "args[" + name() + "].value");
	}
}
