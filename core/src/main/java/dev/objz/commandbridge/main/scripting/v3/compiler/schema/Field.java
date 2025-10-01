package dev.objz.commandbridge.main.scripting.v3.compiler.schema;

import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;

public final class Field<T> {
	private final Rule<T> rule;
	private final String name;

	public Field(String name, Rule<T> rule) {
		this.name = name;
		this.rule = rule;
	}

	public T required(T value, ProblemSink p, Path path) {
		if (value == null) {
			p.error(path.child(name).toString(), "is required");
		}
		return value != null ? value : rule.defaultValue();
	}

	public T orDefault(T value) {
		return value != null ? value : rule.defaultValue();
	}
}
