package dev.objz.commandbridge.main.scripting.v3.compiler.schema;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class Path {
	private final Deque<String> segments = new ArrayDeque<>();

	public static Path root() {
		return new Path();
	}

	public Path child(String name) {
		var p = new Path();
		p.segments.addAll(this.segments);
		p.segments.add(Objects.requireNonNull(name));
		return p;
	}

	public Path index(int i) {
		var p = new Path();
		p.segments.addAll(this.segments);
		p.segments.add("[" + i + "]");
		return p;
	}

	@Override
	public String toString() {
		if (segments.isEmpty())
			return "<root>";
		var sb = new StringBuilder();
		var first = true;
		for (var s : segments) {
			if (s.startsWith("[") && s.endsWith("]")) {
				sb.append(s);
			} else {
				if (!first)
					sb.append('.');
				sb.append(s);
			}
			first = false;
		}
		return sb.toString();
	}
}
