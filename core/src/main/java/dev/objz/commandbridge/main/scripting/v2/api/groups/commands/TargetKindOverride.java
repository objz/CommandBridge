package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;

public record TargetKindOverride(
		TargetKind.Type register,
		TargetKind.Type execute) {
	void validate(ProblemSink problems, int index) {
	}
}
