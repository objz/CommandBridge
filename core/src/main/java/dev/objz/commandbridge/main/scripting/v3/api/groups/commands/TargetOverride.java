package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.Target;

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
