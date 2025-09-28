package dev.objz.commandbridge.main.scripting.v3.api;

import java.util.ArrayList;
import java.util.List;

// this class will collect problems during parsing/validation

public final class ProblemSink {
	private final List<String> errors = new ArrayList<>();

	private final List<String> warnings = new ArrayList<>();

	public void error(String path, String message) {
		errors.add(formatMessage(path, message));
	}

	public void warn(String path, String message) {
		warnings.add(formatMessage(path, message));
	}

	private String formatMessage(String path, String message) {
		return path + ": " + message;
	}

	public List<String> getErrors() {
		return List.copyOf(errors);
	}

	public List<String> getWarnings() {
		return List.copyOf(warnings);
	}
}
