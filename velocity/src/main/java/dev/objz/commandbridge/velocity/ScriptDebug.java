package dev.objz.commandbridge.velocity;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.v3.compiler.io.StepResolver;
import dev.objz.commandbridge.main.scripting.v3.model.domain.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptDebug {
	private static final String NONE = "<none>";
	private static final int MIN_WIDTH = 72;
	private static final int MAX_WIDTH = 160;

	private static final String HEADER_PREFIX = "\u0001SECTION:";

	private ScriptDebug() {
	}

	public static void printScript(Script script, String sourceName) {
		if (!Log.isDebug() || script == null)
			return;

		var lines = new ArrayList<String>();
		String title = "SCRIPT [" + (sourceName == null ? "unknown" : sourceName) + "]";

		section(lines, "Meta", () -> {
			lines.add(base() + "version: " + script.version());
			lines.add(base() + "name: " + q(script.name()));
			lines.add(base() + "enabled: " + script.enabled());
			lines.add(base() + "description: " + orNone(script.description()));
			lines.add(base() + "aliases: " + list(script.aliases()));
		});

		if (script.permissions() != null) {
			var p = script.permissions();
			section(lines, "Permissions", () -> {
				lines.add(base() + "enabled: " + p.enabled());
				lines.add(base() + "silent: " + p.silent());
			});
		}

		var defs = script.defaults();
		section(lines, "Defaults", () -> emitDefaultsYaml(lines, defs));

		section(lines, "Args", () -> {
			var args = script.args();
			if (args == null || args.isEmpty()) {
				lines.add(base() + "[]");
			} else {
				lines.add(base() + "args:");
				for (var a : args) {
					lines.add(indent(1) + "- name: " + q(a.name()));
					lines.add(indent(2) + "required: " + a.required());
					if (a.type() != null)
						lines.add(indent(2) + "type: " + a.type());
				}
			}
		});

		section(lines, "Commands", () -> {
			var steps = script.steps();
			if (steps == null || steps.isEmpty()) {
				lines.add(base() + "[]");
			} else {
				for (var step : steps) {
					var res = StepResolver.resolve(defs, step);
					lines.add(indent(1) + "- command: " + q(step.command()));

					emitEffectiveTargetWithStars(lines, defs, step, res, 2);

					boolean delayStar = step.delayOverride() != null;
					lines.add(indent(2) + "delay: " + dur(res.delay()) + (delayStar ? " *" : ""));

					lines.add(indent(2) + "cooldown: " + dur(res.cooldown()));
				}
			}
		});

		Log.debug("\n" + box(lines, title));
	}

	private static void emitDefaultsYaml(List<String> lines, Defaults d) {
		if (d == null) {
			lines.add(indent(1) + NONE);
			return;
		}
		lines.add(indent(1) + "run-as: " + (d.runAs() == null ? NONE : d.runAs()));
		if (d.id() != null)
			lines.add(indent(1) + "id: " + q(d.id()));

		if (d.kind() != null) {
			lines.add(indent(1) + "kind:");
			lines.add(indent(2) + "register: " + d.kind().register());
			lines.add(indent(2) + "execute: " + d.kind().execute());
		} else {
			lines.add(indent(1) + "kind: " + NONE);
		}

		if (d.server() != null) {
			lines.add(indent(1) + "server:");
			lines.add(indent(2) + "target-required: " + d.server().targetRequired());
			lines.add(indent(2) + "schedule-online: " + d.server().scheduleOnline());
			lines.add(indent(2) + "timeout: " + dur(d.server().timeout()));
			lines.add(indent(2) + "frequency: " + dur(d.server().frequency()));
		} else {
			lines.add(indent(1) + "server: " + NONE);
		}

		lines.add(indent(1) + "delay: " + dur(d.delay()));
		lines.add(indent(1) + "cooldown: " + dur(d.cooldown()));
	}

	private static void emitEffectiveTargetWithStars(
			List<String> lines, Defaults defs, CommandStep step, StepResolver.ResolvedStep res, int ind) {

		lines.add(indent(ind) + "run-as: " + (res.runAs() == null ? NONE : res.runAs()));
		if (res.id() != null)
			lines.add(indent(ind) + "id: " + q(res.id()));

		TargetKind baseKind = defs.kind();
		TargetKind effKind = res.kind();
		TargetKind ovKind = step.kindOverride();

		if (effKind != null) {
			lines.add(indent(ind) + "kind:");
			boolean regStar = ovKind != null && ovKind.register() != null
					&& !Objects.equals(effKind.register(),
							(baseKind == null ? null : baseKind.register()));
			boolean exeStar = ovKind != null && ovKind.execute() != null
					&& !Objects.equals(effKind.execute(),
							(baseKind == null ? null : baseKind.execute()));

			lines.add(indent(ind + 1) + "register: " + effKind.register() + (regStar ? " *" : ""));
			lines.add(indent(ind + 1) + "execute: " + effKind.execute() + (exeStar ? " *" : ""));
		} else {
			lines.add(indent(ind) + "kind: " + NONE);
		}

		TargetServer baseSrv = defs.server();
		TargetServer effSrv = res.server();
		TargetServer ovSrv = step.serverOverride();

		if (effSrv != null) {
			lines.add(indent(ind) + "server:");

			boolean starReq = ovSrv != null && baseSrv != null
					&& (effSrv.targetRequired() != baseSrv.targetRequired());
			boolean starSch = ovSrv != null && baseSrv != null
					&& (effSrv.scheduleOnline() != baseSrv.scheduleOnline());
			boolean starTo = ovSrv != null && baseSrv != null
					&& !Objects.equals(effSrv.timeout(), baseSrv.timeout());
			boolean starFr = ovSrv != null && baseSrv != null
					&& !Objects.equals(effSrv.frequency(), baseSrv.frequency());

			lines.add(indent(ind + 1) + "target-required: " + effSrv.targetRequired()
					+ (starReq ? " *" : ""));
			lines.add(indent(ind + 1) + "schedule-online: " + effSrv.scheduleOnline()
					+ (starSch ? " *" : ""));
			lines.add(indent(ind + 1) + "timeout: " + dur(effSrv.timeout()) + (starTo ? " *" : ""));
			lines.add(indent(ind + 1) + "frequency: " + dur(effSrv.frequency()) + (starFr ? " *" : ""));
		} else {
			lines.add(indent(ind) + "server: " + NONE);
		}
	}

	private static String q(String s) {
		return s == null ? "null" : "\"" + s + "\"";
	}

	private static String list(List<String> l) {
		return (l == null || l.isEmpty()) ? "[]" : l.toString();
	}

	private static String orNone(String s) {
		return s == null || s.isBlank() ? NONE : q(s);
	}

	private static String dur(Duration d) {
		return d == null ? NONE : d.toString();
	}

	private static String base() {
		return indent(1);
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
