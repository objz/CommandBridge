package dev.objz.commandbridge.scripting;

import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.objz.commandbridge.scripting.anno.YmlKey;

public final class DebugPrinter {

	private static final String HEADER_PREFIX = "\u0001SECTION:";
	private static final int MIN_WIDTH = 72;
	private static final int MAX_WIDTH = 160;
	private static final String NONE = "<none>";

	private DebugPrinter() {
	}

	public static String print(Object root) {
		if (root == null)
			return "<null>";

		var lines = new java.util.ArrayList<String>();
		section(lines, root.getClass().getSimpleName(), () -> dump(root, lines, 1, "- "));
		return box(lines, "DEBUG DUMP");
	}

	private static void dump(Object o, List<String> lines, int indent, String listBullet) {
		if (o == null) {
			lines.add(indent(indent) + NONE);
			return;
		}
		Class<?> c = o.getClass();

		if (isScalar(c)) {
			lines.add(indent(indent) + String.valueOf(o));
			return;
		}

		if (o instanceof Map<?, ?> m) {
			for (var e : m.entrySet()) {
				lines.add(indent(indent) + String.valueOf(e.getKey()) + ":");
				dump(e.getValue(), lines, indent + 1, "- ");
			}
			return;
		}

		if (o instanceof Collection<?> col) {
			if (col.isEmpty()) {
				lines.add(indent(indent) + "[]");
				return;
			}
			for (Object e : col) {
				dumpListItem(e, lines, indent);
			}
			return;
		}

		if (c.isRecord()) {
			RecordComponent[] comps = c.getRecordComponents();
			for (RecordComponent rc : comps) {
				Object val = safeGet(rc, o);
				String key = keyOf(rc);
				if (val == null) {
					lines.add(indent(indent) + key + ": " + NONE);
				} else if (isScalar(rc.getType())) {
					lines.add(indent(indent) + key + ": " + val);
				} else {
					lines.add(indent(indent) + key + ":");
					dump(val, lines, indent + 1, "- ");
				}
			}
			return;
		}

		lines.add(indent(indent) + o.toString());
	}

	private static void dumpListItem(Object e, List<String> lines, int indent) {
		if (e == null || isScalar(e.getClass())) {
			lines.add(indent(indent) + "- " + (e == null ? NONE : String.valueOf(e)));
			return;
		}

		if (e.getClass().isRecord()) {
			RecordComponent[] comps = e.getClass().getRecordComponents();
			if (comps.length == 0) {
				lines.add(indent(indent) + "- {}");
				return;
			}
			Object v0 = safeGet(comps[0], e);
			String k0 = keyOf(comps[0]);
			if (v0 == null || isScalar(comps[0].getType())) {
				lines.add(indent(indent) + "- " + k0 + ": " + (v0 == null ? NONE : v0));
			} else {
				lines.add(indent(indent) + "- " + k0 + ":");
				dump(v0, lines, indent + 2, "- ");
			}
			for (int i = 1; i < comps.length; i++) {
				Object vi = safeGet(comps[i], e);
				String ki = keyOf(comps[i]);
				if (vi == null || isScalar(comps[i].getType())) {
					lines.add(indent(indent + 1) + ki + ": " + (vi == null ? NONE : vi));
				} else {
					lines.add(indent(indent + 1) + ki + ":");
					dump(vi, lines, indent + 2, "- ");
				}
			}
			return;
		}

		if (e instanceof Map<?, ?> m) {
			if (m.isEmpty()) {
				lines.add(indent(indent) + "- {}");
				return;
			}
			Iterator<? extends Map.Entry<?, ?>> it = m.entrySet().iterator();
			Map.Entry<?, ?> first = it.next();
			Object v0 = first.getValue();
			String k0 = String.valueOf(first.getKey());
			if (v0 == null || isScalar(v0.getClass())) {
				lines.add(indent(indent) + "- " + k0 + ": " + (v0 == null ? NONE : v0));
			} else {
				lines.add(indent(indent) + "- " + k0 + ":");
				dump(v0, lines, indent + 2, "- ");
			}
			while (it.hasNext()) {
				var en = it.next();
				Object vi = en.getValue();
				String ki = String.valueOf(en.getKey());
				if (vi == null || isScalar(vi.getClass())) {
					lines.add(indent(indent + 1) + ki + ": " + (vi == null ? NONE : vi));
				} else {
					lines.add(indent(indent + 1) + ki + ":");
					dump(vi, lines, indent + 2, "- ");
				}
			}
			return;
		}

		lines.add(indent(indent) + "-");
		dump(e, lines, indent + 1, "- ");
	}

	private static Object safeGet(RecordComponent rc, Object instance) {
		try {
			return rc.getAccessor().invoke(instance);
		} catch (Exception ex) {
			return "<error>";
		}
	}

	private static String keyOf(RecordComponent c) {
		var ann = c.getAnnotation(YmlKey.class);
		return ann != null ? ann.value() : c.getName();
	}

	private static boolean isScalar(Class<?> c) {
		return c.isPrimitive()
				|| Number.class.isAssignableFrom(c)
				|| CharSequence.class.isAssignableFrom(c)
				|| Boolean.class == c
				|| Enum.class.isAssignableFrom(c)
				|| java.time.temporal.TemporalAmount.class.isAssignableFrom(c);
	}

	private static String indent(int n) {
		return "  ".repeat(Math.max(0, n));
	}


	private static void section(List<String> lines, String title, Runnable body) {
		lines.add(HEADER_PREFIX + title);
		body.run();
	}

	private static String box(List<String> lines, String title) {
		int max = title.length() + 4;
		for (String line : lines) {
			String logical = line.startsWith(HEADER_PREFIX) ? line.substring(HEADER_PREFIX.length()) : line;
			max = Math.max(max, logical.length() + 2);
		}
		int width = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, max + 4));

		String border = "─".repeat(width - 2);
		var out = new StringBuilder();
		out.append('┌').append(border).append('┐').append('\n');
		out.append('│').append(center(title, width - 2)).append('│').append('\n');
		out.append('├').append(border).append('┤').append('\n');

		for (String line : lines) {
			if (line.startsWith(HEADER_PREFIX)) {
				String h = line.substring(HEADER_PREFIX.length());
				int inner = width - 2;
				String head = h + " ";
				int dashes = Math.max(0, inner - head.length());
				String rendered = head + "─".repeat(dashes);
				out.append('│').append(rendered).append('│').append('\n');
			} else {
				out.append('│').append(padRight(line, width - 2)).append('│').append('\n');
			}
		}
		out.append('└').append(border).append('┘');
		return out.toString();
	}

	private static String padRight(String s, int n) {
		if (s.length() >= n)
			return s.substring(0, n);
		return s + " ".repeat(n - s.length());
	}

	private static String center(String s, int n) {
		if (s.length() >= n)
			return s.substring(0, n);
		int pad = (n - s.length()) / 2;
		return " ".repeat(pad) + s + " ".repeat(n - s.length() - pad);
	}
}
