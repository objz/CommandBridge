package dev.objz.commandbridge.main.logging;

import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Log {

	private static final AtomicReference<Log> GLOBAL = new AtomicReference<>();
	private static volatile boolean DEBUG = false;

	private final Logger base;
	private final boolean ansi;

	private static final String RESET = "\u001B[0m";
	private static final String GREEN = "\u001B[32m";
	private static final String YELLOW = "\u001B[33m";
	private static final String RED = "\u001B[31m";
	private static final String CYAN = "\u001B[36m";

	private Log(Logger base, boolean ansi) {
		this.base = Objects.requireNonNull(base, "base logger");
		this.ansi = ansi;
	}

	public static void install(Logger injectedVelocityLogger) {
		Objects.requireNonNull(injectedVelocityLogger, "injectedVelocityLogger");
		Log instance = new Log(
				injectedVelocityLogger,
				!"false".equalsIgnoreCase(System.getProperty("cb.ansi", "true"))
		);
		if (!GLOBAL.compareAndSet(null, instance)) {
			throw new IllegalStateException("Log is already installed");
		}
	}

	private static Log get() {
		Log l = GLOBAL.get();
		if (l == null) {
			throw new IllegalStateException("Log is not installed.");
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
			base.error(color(message + " (" + root.getClass().getSimpleName() + ": " + root.getMessage() + ")", RED), args);
		}
	}

	private String color(String msg, String color) {
		if (!ansi || color == null) return msg != null ? msg : "";
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

	private static Throwable rootCause(Throwable t) {
		Throwable c = t;
		while (c.getCause() != null && c.getCause() != c) c = c.getCause();
		return c;
	}
}
