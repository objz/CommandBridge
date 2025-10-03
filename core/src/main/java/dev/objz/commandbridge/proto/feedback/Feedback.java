package dev.objz.commandbridge.proto.feedback;

import java.util.List;

public record Feedback(
		int requested,
		int succeeded,
		int failed,
		List<String> warnings,
		List<String> errors) {
	public static Feedback empty() {
		return new Feedback(0, 0, 0, List.of(), List.of());
	}
}
