package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import java.time.Duration;

import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;

public record TargetServerOverride(
		Boolean targetRequired,
		Boolean scheduleOnline,
		Duration timeout,
		Duration frequency) {
	void validate(ProblemSink problems, int index) {
	}
}
