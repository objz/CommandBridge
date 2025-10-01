package dev.objz.commandbridge.main.scripting.v3.model.domain;

public record TargetKind(Type register, Type execute) {
	public enum Type {
		VELOCITY, BACKEND
	}
}
