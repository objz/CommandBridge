package dev.objz.commandbridge.main.scripting.v3.compiler.problems;

import java.util.List;

public interface ProblemSink {
	void add(Problem problem);

	default void error(String path, String message) {
		add(new Problem(path, Severity.ERROR, message));
	}

	default void warn(String path, String message) {
		add(new Problem(path, Severity.WARN, message));
	}

	default List<Problem> snapshot() {
		return null;
	}

	default boolean hasErrors() {
		var snap = snapshot();
		if (snap == null)
			return false;
		for (var p : snap)
			if (p.severity() == Severity.ERROR)
				return true;
		return false;
	}
}
