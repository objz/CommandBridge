package dev.objz.commandbridge.main.scripting.v3.api.groups.defaults;

import dev.objz.commandbridge.main.scripting.v3.api.Meta;

public record Target(
		Meta<Target.RunAs> runAs,
		Meta<String> id,
		TargetKind targetKind,
		TargetServer targetServer) {
	public enum RunAs {
		CONSOLE, PLAYER, OPERATOR
	}

	public static Target withDefaults() {
		return new Target(
				Meta.isRequired(),
				Meta.isRequired(),
				TargetKind.withDefaults(),
				TargetServer.withDefaults());
	}
}
