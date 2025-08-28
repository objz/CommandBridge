package dev.objz.commandbridge.main.scripting;

import dev.objz.commandbridge.main.scripting.ScriptTypes.*;

import java.time.Duration;
import java.util.List;

public final class Effective {
	private Effective() {
	}

	public static record Script(
			int version,
			ScriptKind kind,
			String name,
			String description,
			boolean enabled,
			List<String> aliases,
			Permissions permissions,
			ExecutorMode runAs,
			Target target,
			Defaults defaults,
			Args args,
			List<Step> steps) {
	}

	public static record Permissions(boolean enabled, boolean silent) {
	}

	public static record Target(
			TargetMode mode,
			String fixed,
			Integer argIndex,
			boolean requireOnline,
			boolean requireOnServer) {
	}

	public static record Defaults(
			Duration delay,
			Duration timeout,
			Duration cooldown,
			RateLimit rateLimit) {
	}

	public static record RateLimit(int count, Duration window, Scope scope) {
		public enum Scope {
			PER_EXECUTOR, PER_SCRIPT
		}
	}

	public static record Args(String description, List<Arg> spec) {
	}

	public static record Arg(
			String name,
			int index,
			boolean required,
			ArgType type,
			Long min,
			Long max,
			List<String> choices,
			String pattern,
			boolean rest) {
	}

	public static record Step(
			String command,
			ExecutorMode runAs,
			Target target,
			Duration delay,
			Duration timeout) {
	}
}
