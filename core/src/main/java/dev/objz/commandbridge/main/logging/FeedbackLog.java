package dev.objz.commandbridge.main.logging;

import dev.objz.commandbridge.main.proto.feedback.Feedback;

public final class FeedbackLog {
	private FeedbackLog() {
	}

	/** Unified summary (no warning count in the summary). */
	public static void summary(String prefix, Feedback f) {
		final String line = String.format(
				"%s%s:%s requested %s%d%s, registered %s%d%s, failed %s%d%s, errors %s%d%s",
				Log.GRAY, prefix, Log.RESET,
				Log.GRAY, f.requested(), Log.RESET,
				(f.succeeded() > 0 ? Log.GREEN : Log.GRAY), f.succeeded(), Log.RESET,
				(f.failed() > 0 ? Log.RED : Log.GREEN), f.failed(), Log.RESET,
				(f.errors() != null && !f.errors().isEmpty() ? Log.RED : Log.GREEN),
				(f.errors() == null ? 0 : f.errors().size()), Log.RESET);
		Log.info(line);
	}

	/** Details: log warnings and errors if present. */
	public static void details(Feedback f) {
		if (f.warnings() != null && !f.warnings().isEmpty()) {
			for (String w : f.warnings())
				Log.info(String.format("%sWarn:%s %s", Log.YELLOW, Log.RESET, w));
		}
		if (f.errors() != null && !f.errors().isEmpty()) {
			for (String e : f.errors())
				Log.info(String.format("%sError:%s %s", Log.RED, Log.RESET, e));
		}
	}
}
