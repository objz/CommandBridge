package dev.objz.commandbridge.main.scripting.v3.model.resolved;

public record Target(
		RunAs runAs,
		String id,
		TargetKind kind,
		TargetServer server) {
	public enum RunAs {
		CONSOLE, PLAYER
	}
}
