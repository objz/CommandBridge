package dev.objz.commandbridge.main.scripting.v3.effective;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import dev.objz.commandbridge.main.scripting.v3.enums.ExecutorMode;
import dev.objz.commandbridge.main.scripting.v3.enums.ScriptSide;

import java.time.Duration;
import java.util.List;

public final class EffectiveModels {
	private EffectiveModels() {
	}

	public static record Script(
			int version,
			String name,
			String description,
			boolean enabled,
			List<String> aliases,
			Permissions permissions,
			Defaults defaults,
			Args args,
			List<Step> steps) {
	}

	public static record Permissions(boolean enabled, boolean silent) {
	}

	public static record Defaults(Target target, Duration delay, Duration cooldown, Duration timeout) {
	}

	public static record Target(
			ExecutorMode runAs,
			String id,
			ScriptSide register,
			ScriptSide execute,
			boolean targetRequired,
			boolean scheduleOnline,
			Duration scheduleTimeout,
			Duration scheduleFrequency) {
	}

	public static record Args(List<Arg> spec) {
	}

	public static record Arg(String name, int index, boolean required, ArgType type, Long min, Long max,
			List<String> choices) {
	}

	public static record Step(String command, Target target, Duration delay, Duration timeout) {
	}
}
