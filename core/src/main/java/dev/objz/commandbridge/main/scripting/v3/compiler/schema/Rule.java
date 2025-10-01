package dev.objz.commandbridge.main.scripting.v3.compiler.schema;

public record Rule<T>(boolean required, T defaultValue) {
	public static <T> Rule<T> isRequired() {
		return new Rule<>(true, null);
	}

	public static <T> Rule<T> isOptional(T def) {
		return new Rule<>(false, def);
	}
}
