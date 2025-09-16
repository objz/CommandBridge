package dev.objz.commandbridge.main.logging;

import dev.objz.commandbridge.main.proto.feedback.Feedback;

public final class FeedbackLog {
	private FeedbackLog() {
	}

	/**
	 * "<Title> stats: requested R, registered S, skipped F, errors E"
	 * - title gray
	 * - requested: ALWAYS emphasized (bold + magenta)
	 * - registered: green if >0 else reset
	 * - skipped (formerly 'failed'): yellow if >0 else reset
	 * - errors: red if >0 else reset
	 */
	public static void summary(String title, Feedback f) {
		int errorsCount = (f.errors() == null) ? 0 : f.errors().size();

		final String line = String.format(
				"%s%s stats:%s requested %s%s%d%s, registered %s%d%s, skipped %s%d%s, errors %s%d%s",
				Log.GRAY, title, Log.RESET,
				Log.BOLD, Log.MAGENTA, f.requested(), Log.RESET,
				colorIf(f.succeeded() > 0, Log.GREEN), f.succeeded(), Log.RESET,
				colorIf(f.failed() > 0, Log.YELLOW), f.failed(), Log.RESET,
				colorIf(errorsCount > 0, Log.RED), errorsCount, Log.RESET);
		Log.info(line);
	}

	public static void details(Feedback f) {
		if (f.warnings() != null && !f.warnings().isEmpty()) {
			for (String w : f.warnings()) {
				Log.info(String.format("%sWarn:%s %s", Log.YELLOW, Log.RESET, w));
			}
		}
		if (f.errors() != null && !f.errors().isEmpty()) {
			for (String e : f.errors()) {
				Log.info(String.format("%sError:%s %s", Log.RED, Log.RESET, e));
			}
		}
	}

	private static String colorIf(boolean cond, String color) {
		return cond ? color : "";
	}
}
