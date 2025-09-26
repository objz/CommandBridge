package dev.objz.commandbridge.main.scripting.v3.util;

import java.time.Duration;
import java.util.Locale;

public final class DurationUtil {
	private DurationUtil() {
	}

	public static Duration parseFlexible(String s, Duration fallback) {
		if (s == null || s.isBlank())
			return fallback;
		s = s.trim().toLowerCase(Locale.ROOT);
		if (s.matches("^[0-9]+$"))
			return Duration.ofSeconds(Long.parseLong(s));
		if (s.endsWith("ms"))
			return Duration.ofMillis(Long.parseLong(s.substring(0, s.length() - 2)));
		if (s.endsWith("s"))
			return Duration.ofSeconds(Long.parseLong(s.substring(0, s.length() - 1)));
		if (s.endsWith("m"))
			return Duration.ofMinutes(Long.parseLong(s.substring(0, s.length() - 1)));
		if (s.endsWith("h"))
			return Duration.ofHours(Long.parseLong(s.substring(0, s.length() - 1)));
		if (s.endsWith("d"))
			return Duration.ofDays(Long.parseLong(s.substring(0, s.length() - 1)));
		throw new IllegalArgumentException("Invalid duration: " + s);
	}
}
