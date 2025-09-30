package dev.objz.commandbridge.main.scripting.v3.model.resolved;

public record TargetKind(Type register, Type execute) {
	public enum Type {
		VELOCITY, BACKEND
	}
}
