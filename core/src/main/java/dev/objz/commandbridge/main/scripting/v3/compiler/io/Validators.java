package dev.objz.commandbridge.main.scripting.v3.compiler.io;

import java.time.Duration;
import java.util.Locale;

import dev.objz.commandbridge.main.scripting.v3.compiler.problems.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.compiler.schema.Path;

public final class Validators {
	private Validators() {
	}

	public static void positive(Duration d, Path p, ProblemSink sink, String field) {
		if (d == null)
			return;
		if (d.isZero() || d.isNegative()) {
			sink.error(p.child(field).toString(), "must be positive");
		}
	}

	public static void nonNegative(Duration d, Path p, ProblemSink sink, String field) {
		if (d == null)
			return;
		if (d.isNegative()) {
			sink.error(p.child(field).toString(), "must not be negative");
		}
	}

	public static <E extends Enum<E>> E parseEnum(
			Class<E> e, String in, ProblemSink s, Path p, String field, E fallback) {
		if (in == null) {
			s.error(p.child(field).toString(), "is required");
			return fallback;
		}
		try {
			return Enum.valueOf(e, in.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			s.error(p.child(field).toString(), "unknown: " + in);
			return fallback;
		}
	}

	public static <E extends Enum<E>> E requiredEnum(
			Class<E> type, String raw, ProblemSink p, Path path, String field) {
		if (raw == null) {
			p.error(path.child(field).toString(), "is required");
			return null;
		}
		return parseEnum(type, raw, p, path, field, null);
	}
}
