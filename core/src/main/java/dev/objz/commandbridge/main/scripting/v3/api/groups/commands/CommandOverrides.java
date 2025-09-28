package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import java.time.Duration;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;

public record CommandOverrides(
		TargetOverride target,
		Duration delay,
		Duration cooldown) {
	public void validate(ProblemSink problems, int index) {
		if (target != null)
			target.validate(problems, index);
	}

	public boolean hasAnyOverride() {
		return target != null || delay != null || cooldown != null;
	}
}
