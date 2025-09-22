package dev.objz.commandbridge.main.logging;

import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Log {

	private static final AtomicReference<Log> GLOBAL = new AtomicReference<>();
	private static volatile boolean DEBUG = true;

	private static volatile BooleanSupplier isMainThread;
	private static volatile Consumer<Runnable> mainThreadDispatch;

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
		this.base = Objects.requireNonNull(base, "logger");
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

	public static void install(java.util.logging.Logger julLogger) {
		Objects.requireNonNull(julLogger, "julLogger");
		install(new org.slf4j.helpers.NOPLoggerFactory() {
			private final org.slf4j.Logger bridged = new org.slf4j.Logger() {
				@Override
				public String getName() {
					return julLogger.getName();
				}

				private void log(java.util.logging.Level lvl, String fmt, Object... args) {
					String msg = (fmt == null) ? ""
							: org.slf4j.helpers.MessageFormatter.arrayFormat(fmt, args)
									.getMessage();
					julLogger.log(lvl, msg);
				}

				@Override
				public boolean isTraceEnabled() {
					return false;
				}

				@Override
				public void trace(String msg) {
				}

				@Override
				public void trace(String format, Object arg) {
				}

				@Override
				public void trace(String format, Object arg1, Object arg2) {
				}

				@Override
				public void trace(String format, Object... arguments) {
				}

				@Override
				public void trace(String msg, Throwable t) {
				}

				@Override
				public boolean isDebugEnabled() {
					return true;
				}

				@Override
				public void debug(String msg) {
					log(java.util.logging.Level.FINE, msg);
				}

				@Override
				public void debug(String format, Object arg) {
					log(java.util.logging.Level.FINE, format, arg);
				}

				@Override
				public void debug(String format, Object arg1, Object arg2) {
					log(java.util.logging.Level.FINE, format, arg1, arg2);
				}

				@Override
				public void debug(String format, Object... arguments) {
					log(java.util.logging.Level.FINE, format, arguments);
				}

				@Override
				public void debug(String msg, Throwable t) {
					julLogger.log(java.util.logging.Level.FINE, msg, t);
				}

				@Override
				public boolean isInfoEnabled() {
					return true;
				}

				@Override
				public void info(String msg) {
					log(java.util.logging.Level.INFO, msg);
				}

				@Override
				public void info(String format, Object arg) {
					log(java.util.logging.Level.INFO, format, arg);
				}

				@Override
				public void info(String format, Object arg1, Object arg2) {
					log(java.util.logging.Level.INFO, format, arg1, arg2);
				}

				@Override
				public void info(String format, Object... arguments) {
					log(java.util.logging.Level.INFO, format, arguments);
				}

				@Override
				public void info(String msg, Throwable t) {
					julLogger.log(java.util.logging.Level.INFO, msg, t);
				}

				@Override
				public boolean isWarnEnabled() {
					return true;
				}

				@Override
				public void warn(String msg) {
					log(java.util.logging.Level.WARNING, msg);
				}

				@Override
				public void warn(String format, Object arg) {
					log(java.util.logging.Level.WARNING, format, arg);
				}

				@Override
				public void warn(String format, Object arg1, Object arg2) {
					log(java.util.logging.Level.WARNING, format, arg1, arg2);
				}

				@Override
				public void warn(String format, Object... arguments) {
					log(java.util.logging.Level.WARNING, format, arguments);
				}

				@Override
				public void warn(String msg, Throwable t) {
					julLogger.log(java.util.logging.Level.WARNING, msg, t);
				}

				@Override
				public boolean isErrorEnabled() {
					return true;
				}

				@Override
				public void error(String msg) {
					log(java.util.logging.Level.SEVERE, msg);
				}

				@Override
				public void error(String format, Object arg) {
					log(java.util.logging.Level.SEVERE, format, arg);
				}

				@Override
				public void error(String format, Object arg1, Object arg2) {
					log(java.util.logging.Level.SEVERE, format, arg1, arg2);
				}

				@Override
				public void error(String format, Object... arguments) {
					log(java.util.logging.Level.SEVERE, format, arguments);
				}

				@Override
				public void error(String msg, Throwable t) {
					julLogger.log(java.util.logging.Level.SEVERE, msg, t);
				}

				@Override
				public boolean isTraceEnabled(org.slf4j.Marker marker) {
					return false;
				}

				@Override
				public void trace(org.slf4j.Marker marker, String s) {
				}

				@Override
				public void trace(org.slf4j.Marker marker, String s, Object o) {
				}

				@Override
				public void trace(org.slf4j.Marker marker, String s, Object o, Object o1) {
				}

				@Override
				public void trace(org.slf4j.Marker marker, String s, Object... objects) {
				}

				@Override
				public void trace(org.slf4j.Marker marker, String s, Throwable throwable) {
				}

				@Override
				public boolean isDebugEnabled(org.slf4j.Marker marker) {
					return isDebugEnabled();
				}

				@Override
				public void debug(org.slf4j.Marker marker, String s) {
					debug(s);
				}

				@Override
				public void debug(org.slf4j.Marker marker, String s, Object o) {
					debug(s, o);
				}

				@Override
				public void debug(org.slf4j.Marker marker, String s, Object o, Object o1) {
					debug(s, o, o1);
				}

				@Override
				public void debug(org.slf4j.Marker marker, String s, Object... objects) {
					debug(s, objects);
				}

				@Override
				public void debug(org.slf4j.Marker marker, String s, Throwable throwable) {
					debug(s);
				}

				@Override
				public boolean isInfoEnabled(org.slf4j.Marker marker) {
					return isInfoEnabled();
				}

				@Override
				public void info(org.slf4j.Marker marker, String s) {
					info(s);
				}

				@Override
				public void info(org.slf4j.Marker marker, String s, Object o) {
					info(s, o);
				}

				@Override
				public void info(org.slf4j.Marker marker, String s, Object o, Object o1) {
					info(s, o, o1);
				}

				@Override
				public void info(org.slf4j.Marker marker, String s, Object... objects) {
					info(s, objects);
				}

				@Override
				public void info(org.slf4j.Marker marker, String s, Throwable throwable) {
					info(s);
				}

				@Override
				public boolean isWarnEnabled(org.slf4j.Marker marker) {
					return isWarnEnabled();
				}

				@Override
				public void warn(org.slf4j.Marker marker, String s) {
					warn(s);
				}

				@Override
				public void warn(org.slf4j.Marker marker, String s, Object o) {
					warn(s, o);
				}

				@Override
				public void warn(org.slf4j.Marker marker, String s, Object o, Object o1) {
					warn(s, o, o1);
				}

				@Override
				public void warn(org.slf4j.Marker marker, String s, Object... objects) {
					warn(s, objects);
				}

				@Override
				public void warn(org.slf4j.Marker marker, String s, Throwable throwable) {
					warn(s);
				}

				@Override
				public boolean isErrorEnabled(org.slf4j.Marker marker) {
					return isErrorEnabled();
				}

				@Override
				public void error(org.slf4j.Marker marker, String s) {
					error(s);
				}

				@Override
				public void error(org.slf4j.Marker marker, String s, Object o) {
					error(s, o);
				}

				@Override
				public void error(org.slf4j.Marker marker, String s, Object o, Object o1) {
					error(s, o, o1);
				}

				@Override
				public void error(org.slf4j.Marker marker, String s, Object... objects) {
					error(s, objects);
				}

				@Override
				public void error(org.slf4j.Marker marker, String s, Throwable throwable) {
					error(s, throwable);
				}
			};

			@Override
			public org.slf4j.Logger getLogger(String name) {
				return bridged;
			}
		}.getLogger(julLogger.getName()));
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
		onConsole(l -> l.info(color(message, null), args));
	}

	private void warnI(String message, Object... args) {
		onConsole(l -> l.warn(color(message, YELLOW), args));
	}

	private void errorI(String message, Object... args) {
		onConsole(l -> l.error(color(message, RED), args));
	}

	private void successI(String message, Object... args) {
		onConsole(l -> l.info(color(message, GREEN), args));
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

	public static void installThreadMarshalling(
			java.util.function.BooleanSupplier isMain,
			java.util.function.Consumer<Runnable> dispatch) {
		isMainThread = Objects.requireNonNull(isMain, "isMain");
		mainThreadDispatch = Objects.requireNonNull(dispatch, "dispatch");
	}

	private void onConsole(java.util.function.Consumer<Logger> sink) {
		var isMain = isMainThread;
		var dispatch = mainThreadDispatch;
		if (isMain != null && dispatch != null && !isMain.getAsBoolean()) {
			dispatch.accept(() -> sink.accept(base));
		} else {
			sink.accept(base);
		}
	}
}
