package dev.objz.commandbridge.main.scripting.v3.model.domain;

import java.time.Duration;

public record Defaults(
		RunAs runAs,
		String id,
		TargetKind kind,
		TargetServer server,
		Duration delay,
		Duration cooldown) {

	public enum RunAs {
		CONSOLE, PLAYER
	}
}
