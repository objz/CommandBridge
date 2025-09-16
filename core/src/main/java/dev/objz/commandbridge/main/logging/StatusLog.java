package dev.objz.commandbridge.main.logging;

public final class StatusLog {
	private StatusLog() {
	}

	/**
	 * "Scripts summary: loaded L, enabled E, disabled D, errors X"
	 * - label gray
	 * - loaded: ALWAYS emphasized (bold + magenta)
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

	/**
	 * "Register stubs: pushed N command stub(s) to 'backendId'"
	 * - label gray
	 * - N emphasized (bold + magenta), never green (not a success yet)
	 * - backend id gray
	 */
	public static void registerPushed(int count, String backendId) {
		final String msg = String.format(
				"%sRegister stubs:%s pushed %s%s%d%s command stub%s to %s'%s'%s",
				Log.GRAY, Log.RESET,
				Log.BOLD, Log.MAGENTA, count, Log.RESET, (count == 1 ? "" : "s"),
				Log.GRAY, backendId, Log.RESET);
		Log.info(msg);
	}


	private static String colorIf(boolean cond, String color) {
		return cond ? color : ""; 
	}
}
