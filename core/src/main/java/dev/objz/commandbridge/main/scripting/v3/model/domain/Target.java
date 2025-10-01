package dev.objz.commandbridge.main.scripting.v3.model.domain;

public record Target(
		RunAs runAs,
		String id,
		TargetKind kind,
		TargetServer server) {
	public enum RunAs {
		CONSOLE, PLAYER
	}
}
