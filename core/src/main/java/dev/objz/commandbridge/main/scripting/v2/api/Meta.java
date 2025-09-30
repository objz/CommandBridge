package dev.objz.commandbridge.main.scripting.v3.api;

// Meta information for every field
// required, default and the value
// every value will implement this interface

// public interface Meta<T> {
// 	boolean required();
//
// 	T defaultValue();
//
// 	T value();
// }
//

public record Meta<T>(
		boolean required,
		T defaultValue,
		T value) {
	public static <T> Meta<T> isRequired() {
		return new Meta<>(true, null, null);
	}

	public static <T> Meta<T> isOptional(T defaultValue, T value) {
		return new Meta<>(false, defaultValue, value);
	}

	public static <T> Meta<T> isOptional(T defaultValue) {
		return new Meta<>(false, defaultValue, defaultValue);
	}

	public T effectiveValue() {
		return value != null ? value : defaultValue;
	}

	public boolean hasValue() {
		return value != null;
	}
}
