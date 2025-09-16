package dev.objz.commandbridge.main.logging;

import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Log {

	private static final AtomicReference<Log> GLOBAL = new AtomicReference<>();
	private static volatile boolean DEBUG = true;

	private final Logger base;
	private final boolean ansi;

	public static final String RESET = "\u001B[0m";
	public static final String GREEN = "\u001B[32m";
	public static final String YELLOW = "\u001B[33m";
	public static final String RED = "\u001B[31m";
	public static final String CYAN = "\u001B[36m";
	public static final String GRAY = "\u001B[37m";
	public static final String MAGENTA = "\u001B[35m";
	public static final String BOLD = "\u001B[1m";

	private Log(Logger base, boolean ansi) {
		this.base = Objects.requireNonNull(base, "base logger");
		this.ansi = ansi;
	}

	public static void install(Logger slf4JLogger) {
		Objects.requireNonNull(slf4JLogger, "injectedVelocityLogger");
		Log instance = new Log(
				slf4JLogger,
				!"false".equalsIgnoreCase(System.getProperty("cb.ansi", "true")));
		if (!GLOBAL.compareAndSet(null, instance)) {
			throw new IllegalStateException("Log is already installed");
		}
	}

	private static Log get() {
		Log l = GLOBAL.get();
		if (l == null) {
			throw new IllegalStateException("Log is not installed");
		}
		return l;
	}

	public static void setDebug(boolean enabled) {
		DEBUG = enabled;
	}

	public static boolean isDebug() {
		return DEBUG;
	}

	public static void info(String msg, Object... args) {
		get().infoI(msg, args);
	}

	public static void warn(String msg, Object... args) {
		get().warnI(msg, args);
	}

	public static void error(String msg, Object... args) {
		get().errorI(msg, args);
	}

	public static void success(String msg, Object... args) {
		get().successI(msg, args);
	}

	public static void success(boolean highlight, String msg, Object... args) {
		Log self = get();
		if (!highlight || args == null || args.length == 0 || !self.ansi) {
			self.successI(msg, args); 
			return;
		}

		String rendered = self.renderHighlightedSuccess(msg, args);
		self.base.info(rendered);
	}

	public static void debug(String msg, Object... args) {
		get().debugI(msg, args);
	}

	public static void error(Throwable t, String msg, Object... args) {
		get().errorI(t, msg, args);
	}

	private void infoI(String message, Object... args) {
		base.info(color(message, null), args);
	}

	private void warnI(String message, Object... args) {
		base.warn(color(message, YELLOW), args);
	}

	private void errorI(String message, Object... args) {
		base.error(color(message, RED), args);
	}

	private void successI(String message, Object... args) {
		base.info(color(message, GREEN), args);
	}

	private void debugI(String message, Object... args) {
		if (DEBUG) {
			String ext = extendedPrefix(); // cyan
			String fullMsg = (ansi ? CYAN + ext + RESET : ext) + " " + (message != null ? message : "");
			base.info(fullMsg, args);
		}
	}

	private void errorI(Throwable t, String message, Object... args) {
		if (DEBUG) {
			base.error(color(message, RED), args, t);
		} else {
			Throwable root = rootCause(t);
			base.error(color(message + " (" + root.getClass().getSimpleName() + ": " + root.getMessage()
					+ ")", RED), args);
		}
	}

	private String color(String msg, String color) {
		if (!ansi || color == null)
			return msg != null ? msg : "";
		return color + (msg != null ? msg : "") + RESET;
	}

	private String extendedPrefix() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (StackTraceElement e : stack) {
			String cn = e.getClassName();
			if (!cn.equals(Log.class.getName()) && !cn.startsWith("java.lang.Thread")) {
				return "(" + cn + "#" + e.getMethodName() + "):";
			}
		}
		return "";
	}

	private String renderHighlightedSuccess(String template, Object... args) {
		final String GREEN = Log.GREEN, GRAY = Log.GRAY, RESET = Log.RESET;
		StringBuilder sb = new StringBuilder(template.length() + 32);
		sb.append(GREEN);

		int pos = 0, argIdx = 0, n = template.length();
		while (argIdx < args.length) {
			int i = template.indexOf("{}", pos);
			if (i < 0)
				break; 

			// Is it exactly `'{}'` 
			boolean hasLeading = (i - 1) >= 0 && template.charAt(i - 1) == '\'';
			boolean hasTrailing = (i + 2) < n && template.charAt(i + 2) == '\'';
			boolean quoted = hasLeading && hasTrailing;

			if (quoted) {
				sb.append(template, pos, i - 1);
				sb.append(GRAY).append('\'').append(String.valueOf(args[argIdx++])).append('\'')
						.append(RESET).append(GREEN);
				pos = i + 3; 
			} else {
				sb.append(template, pos, i);
				sb.append(GRAY).append(String.valueOf(args[argIdx++])).append(RESET).append(GREEN);
				pos = i + 2; // skip {}
			}
		}

		if (pos < n)
			sb.append(template, pos, n);

		sb.append(RESET);
		return sb.toString();
	}

	private static Throwable rootCause(Throwable t) {
		Throwable c = t;
		while (c.getCause() != null && c.getCause() != c)
			c = c.getCause();
		return c;
	}
}
