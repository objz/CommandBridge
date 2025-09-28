package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import java.time.Duration;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.Target;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;

public record TargetOverride(
		Target.RunAs runAs,
		String id,
		TargetKindOverride kind,
		TargetServerOverride server
// null => use default
) {
	public void validate(ProblemSink problems, int index) {
		if (kind != null)
			kind.validate(problems, index);
		if (server != null)
			server.validate(problems, index);
	}
}

record TargetKindOverride(
		TargetKind.Type register,
		TargetKind.Type execute) {
	void validate(ProblemSink problems, int index) {
	}
}

record TargetServerOverride(
		Boolean targetRequired,
		Boolean scheduleOnline,
		Duration timeout,
		Duration frequency) {
	void validate(ProblemSink problems, int index) {
	}
}
