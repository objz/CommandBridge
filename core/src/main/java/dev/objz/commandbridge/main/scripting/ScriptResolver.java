package dev.objz.commandbridge.main.scripting;

import dev.objz.commandbridge.main.scripting.ScriptTypes.*;
import dev.objz.commandbridge.main.scripting.model.Spec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ScriptResolver {
	private final Schema.Registry schema;

	public ScriptResolver(Schema.Registry schema) {
		this.schema = schema;
	}

	public static final class ValidationException extends RuntimeException {
		private final String scriptName;
		private final List<String> errors;

		public ValidationException(String scriptName, List<String> errors) {
			super("Script '" + scriptName + "' invalid");
			this.scriptName = scriptName;
			this.errors = List.copyOf(errors);
		}

		public String scriptName() {
			return scriptName;
		}

		public List<String> errors() {
			return errors;
		}
	}

	public Effective.Script resolve(Spec.ScriptSpecV2 s) {
		var errs = new ArrayList<String>();
		if (s == null)
			throw new IllegalArgumentException("script is null");
		var ctx = schema.ctx(s.name() != null ? s.name() : "<unknown>");

		require("version", s.version(), errs);
		require("kind", s.kind(), errs);
		require("name", s.name(), errs);
		require("commands", s.commands(), errs);

		int version = def("version", s.version(), 2, ctx);
		ScriptKind kind = def("kind", s.kind(), ScriptKind.VELOCITY_TO_BACKEND, ctx);
		String name = def("name", s.name(), null, ctx);
		String description = def("description", s.description(), "", ctx);
		boolean enabled = def("enabled", s.enabled(), Boolean.TRUE, ctx);
		List<String> aliases = def("aliases", s.aliases(), List.of(), ctx);

		Effective.Permissions perms = resolvePermissions(s.permissions(), ctx);
		ExecutorMode runAs = def("run-as", s.runAs(), ExecutorMode.PLAYER, ctx);

		Effective.Target globalTarget = resolveTarget("target", s.target(), ctx, errs);
		Effective.Defaults defaults = resolveDefaults(s.defaults(), ctx, errs);
		Effective.Args args = resolveArgs(s.args(), ctx);

		List<Effective.Step> steps = new ArrayList<>();
		for (int i = 0; i < s.commands().size(); i++) {
			var step = s.commands().get(i);
			String base = "commands[" + i + "]";
			require(base + ".command", step.command(), errs);

			ExecutorMode stepRunAs = pick(step.runAs(), runAs);
			Effective.Target stepTarget = step.target() != null
					? resolveTarget(base + ".target", step.target(), ctx, errs)
					: globalTarget;
			Duration stepDelay = pickDuration(step.delay(), defaults.delay());
			Duration stepTimeout = pickDuration(step.timeout(), defaults.timeout());

			steps.add(new Effective.Step(step.command(), stepRunAs, stepTarget, stepDelay, stepTimeout));
		}

		if (!errs.isEmpty()) {
			throw new ValidationException(name != null ? name : "<unnamed>", errs);
		}

		return new Effective.Script(
				version, kind, name, description, enabled, aliases,
				perms, runAs, globalTarget, defaults, args, steps);
	}

	private Effective.Permissions resolvePermissions(Spec.Permissions p, Schema.FieldRule.Context ctx) {
		boolean enabled = def("permissions.enabled", p != null ? p.enabled() : null, Boolean.TRUE, ctx);
		boolean silent = def("permissions.silent", p != null ? p.silent() : null, Boolean.FALSE, ctx);
		return new Effective.Permissions(enabled, silent);
	}

	private Effective.Target resolveTarget(String base, Spec.TargetSpec t, Schema.FieldRule.Context ctx,
			List<String> errs) {
		TargetMode mode = def(base + ".mode", t != null ? t.mode() : null, TargetMode.PLAYERS_SERVER, ctx);
		String fixed = def(base + ".fixed", t != null ? t.fixed() : null, null, ctx);
		Integer idx = def(base + ".arg-index", t != null ? t.argIndex() : null, null, ctx);
		boolean reqOnl = def(base + ".require-online", t != null ? t.requireOnline() : null, Boolean.TRUE, ctx);
		boolean reqSrv = def(base + ".require-on-server", t != null ? t.requireOnServer() : null, Boolean.TRUE,
				ctx);

		switch (mode) {
			case FIXED -> {
				if (isBlank(fixed))
					errs.add(base + ".fixed required when mode=FIXED");
			}
			case BY_ARG -> {
				if (idx == null)
					errs.add(base + ".arg-index required when mode=BY_ARG");
			}
			case PLAYERS_SERVER -> {
				/* ok */ }
		}
		return new Effective.Target(mode, fixed, idx, reqOnl, reqSrv);
	}

	private Effective.Defaults resolveDefaults(Spec.Defaults d, Schema.FieldRule.Context ctx, List<String> errs) {
		Duration delay = parseOrDefault("defaults.delay", d != null ? d.delay() : null, Duration.ZERO, ctx,
				errs);
		Duration timeout = parseOrDefault("defaults.timeout", d != null ? d.timeout() : null,
				Duration.ofSeconds(2), ctx, errs);
		Duration cooldown = parseOrDefault("defaults.cooldown", d != null ? d.cooldown() : null, Duration.ZERO,
				ctx, errs);
		Effective.RateLimit rl = parseRateLimit(
				def("defaults.rate-limit", d != null ? d.rateLimit() : null, null, ctx), errs);
		return new Effective.Defaults(delay, timeout, cooldown, rl);
	}

	private Effective.Args resolveArgs(Spec.ArgsBlock a, Schema.FieldRule.Context ctx) {
		if (a == null || a.spec() == null || a.spec().isEmpty())
			return new Effective.Args(def("args.description", a != null ? a.description() : null, "", ctx),
					List.of());

		List<Effective.Arg> out = new ArrayList<>();
		int idx = 0;
		for (var s : a.spec()) {
			String name = pick(s.name(), "arg" + idx);
			int index = pick(s.index(), idx);
			boolean required = def("args.spec[].required", s.required(), Boolean.TRUE, ctx);
			var type = pick(s.type(), ScriptTypes.ArgType.WORD);
			Long min = s.min();
			Long max = s.max();
			List<String> choices = pick(s.choices(), List.of());
			String pattern = s.pattern();
			boolean rest = def("args.spec[].rest", s.rest(), Boolean.FALSE, ctx);
			out.add(new Effective.Arg(name, index, required, type, min, max, choices, pattern, rest));
			idx++;
		}
		String desc = def("args.description", a.description(), "", ctx);
		return new Effective.Args(desc, out);
	}

	private Effective.RateLimit parseRateLimit(String raw, List<String> errs) {
		if (raw == null || raw.isBlank())
			return null;
		try {
			String[] parts = raw.trim().split("\\s+");
			if (parts.length != 2)
				throw new IllegalArgumentException("expected '<count>/<window> <scope>'");
			String[] c = parts[0].split("/");
			int count = Integer.parseInt(c[0]);
			Duration window = parseDuration(c[1]);
			var scope = parts[1].toLowerCase(Locale.ROOT).contains("executor")
					? Effective.RateLimit.Scope.PER_EXECUTOR
					: Effective.RateLimit.Scope.PER_SCRIPT;
			return new Effective.RateLimit(count, window, scope);
		} catch (Exception e) {
			errs.add("invalid rate-limit '" + raw + "'");
			return null;
		}
	}

	private void require(String path, Object value, List<String> errs) {
		var r = schema.ruleFor(path);
		if (r.isPresent() && r.get().isRequired() && isNullOrEmpty(value)) {
			errs.add(path + " is required");
		}
	}

	private <T> T def(String path, T got, T fallback, Schema.FieldRule.Context ctx) {
		if (!isNullOrEmpty(got))
			return got;
		var r = schema.ruleFor(path);
		if (r.isPresent()) {
			var typed = (Schema.FieldRule<T>) r.get();
			T def = typed.defaultValue(ctx);
			return def != null ? def : fallback;
		}
		return fallback;
	}

	private static boolean isNullOrEmpty(Object v) {
		if (v == null)
			return true;
		if (v instanceof String s)
			return s.isBlank();
		if (v instanceof List<?> l)
			return l.isEmpty();
		return false;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static <T> T pick(T local, T global) {
		return local != null ? local : global;
	}

	private static Duration pickDuration(String raw, Duration def) {
		if (raw == null || raw.isBlank())
			return def;
		return parseDuration(raw);
	}

	private static Duration parseDuration(String s) {
		s = s.trim().toLowerCase();
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
		throw new IllegalArgumentException("invalid duration: " + s);
	}

	private Duration parseOrDefault(String path, String raw, Duration def, Schema.FieldRule.Context ctx,
			List<String> errs) {
		String val = def(path, raw, null, ctx);
		if (val == null || val.isBlank())
			return def;
		try {
			return parseDuration(val);
		} catch (IllegalArgumentException e) {
			errs.add("invalid " + path + " '" + val + "'");
			return def;
		}
	}
}
