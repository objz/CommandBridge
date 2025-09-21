package dev.objz.commandbridge.main.logging;

import dev.objz.commandbridge.main.proto.feedback.Feedback;

public final class FeedbackLog {
	private FeedbackLog() {
	}

	public static void summary(String title, Feedback f, String from) {
		int errorsCount = (f.errors() == null) ? 0 : f.errors().size();

		final String line = String.format(
				"%s%s @%s:%s requested %s%s%d%s, registered %s%d%s, skipped %s%d%s, errors %s%d%s",
				Log.GRAY, title, from, Log.RESET,
				Log.BOLD, Log.MAGENTA, f.requested(), Log.RESET,
				colorIf(f.succeeded() > 0, Log.GREEN), f.succeeded(), Log.RESET,
				colorIf(f.failed() > 0, Log.YELLOW), f.failed(), Log.RESET,
				colorIf(errorsCount > 0, Log.RED), errorsCount, Log.RESET);
		Log.info(line);
	}

	public static void details(Feedback f, String from) {
		details(f, from, false);
	}

	public static void details(Feedback f, String from, boolean backendMode) {
		if (f.warnings() != null && !f.warnings().isEmpty()) {
			for (String w : f.warnings()) {
				if (backendMode) {
					Log.warn(w);
				} else {
					Log.warn("@" + from + ": " + w);
				}
			}
		}
		if (f.errors() != null && !f.errors().isEmpty()) {
			for (String e : f.errors()) {
				if (backendMode) {
					Log.error(e);
				} else {
					Log.error("@" + from + ": " + e);
				}
			}
		}
	}

	private static String colorIf(boolean cond, String color) {
		return cond ? color : "";
	}
}
