package dev.objz.commandbridge.main.scripting.v3.api.groups.defaults;

import dev.objz.commandbridge.main.scripting.v3.api.Meta;

public record TargetKind(
		Meta<Type> register,
		Meta<Type> execute) {

	public enum Type {
		VELOCITY, BACKEND
	}

	public static TargetKind withDefaults() {
		return new TargetKind(
				Meta.isRequired(),
				Meta.isRequired());
	}
}
