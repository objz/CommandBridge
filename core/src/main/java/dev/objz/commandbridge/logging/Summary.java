package dev.objz.commandbridge.logging;

import dev.objz.commandbridge.net.payloads.feedback.Feedback;

public final class Summary {
    private Summary() {
    }

    /**
     * summary: loaded L, enabled E, disabled D, errors X"
     * - enabled: green if >0 else reset
     * - disabled: yellow if >0 else reset
     * - errors: red if >0 else reset
     */
    public static void scriptsSummary(long loaded, long enabled, long disabled, long errors) {
        final String msg = String.format(
                "%sScripts summary:%s loaded %s%s%d%s, enabled %s%d%s, disabled %s%d%s, errors %s%d%s",
                Log.GRAY, Log.RESET,
                Log.BOLD, Log.MAGENTA, loaded, Log.RESET,
                colorIf(enabled > 0, Log.GREEN), enabled, Log.RESET,
                colorIf(disabled > 0, Log.YELLOW), disabled, Log.RESET,
                colorIf(errors > 0, Log.RED), errors, Log.RESET);
        Log.info(msg);
    }

    public static void feedbackSummary(String title, Feedback f, String from) {
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

    public static void feedbackDetails(Feedback f, String from, boolean backendMode) {
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
