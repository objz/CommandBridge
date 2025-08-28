package dev.objz.commandbridge.main.scripting.model;

import dev.objz.commandbridge.main.scripting.ScriptTypes.*;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

public final class Spec {
	private Spec() {
	}

	@ConfigSerializable
	public static record ScriptSpecV2(
			@Setting("version") Integer version,
			@Setting("kind") ScriptKind kind,
			@Setting("name") String name,
			@Setting("description") String description,
			@Setting("enabled") Boolean enabled,
			@Setting("aliases") List<String> aliases,
			@Setting("permissions") Permissions permissions,
			@Setting("run-as") ExecutorMode runAs,
			@Setting("target") TargetSpec target,
			@Setting("defaults") Defaults defaults,
			@Setting("args") ArgsBlock args,
			@Setting("commands") List<CommandStep> commands) {
	}

	@ConfigSerializable
	public static record Permissions(
			@Setting("enabled") Boolean enabled,
			@Setting("silent") Boolean silent) {
	}

	@ConfigSerializable
	public static record TargetSpec(
			@Setting("mode") TargetMode mode, // PLAYERS_SERVER | FIXED | BY_ARG
			@Setting("fixed") String fixed, // when FIXED
			@Setting("arg-index") Integer argIndex, // when BY_ARG
			@Setting("require-online") Boolean requireOnline,
			@Setting("require-on-server") Boolean requireOnServer) {
	}

	@ConfigSerializable
	public static record Defaults(
			@Setting("delay") String delay, // "0" | "500ms" | "2s"
			@Setting("timeout") String timeout,
			@Setting("cooldown") String cooldown,
			@Setting("rate-limit") String rateLimit // "5/10s per-executor"
	) {
	}

	@ConfigSerializable
	public static record ArgsBlock(
			@Setting("description") String description,
			@Setting("spec") List<ArgSpec> spec) {
	}

	@ConfigSerializable
	public static record ArgSpec(
			@Setting("name") String name,
			@Setting("index") Integer index,
			@Setting("required") Boolean required, // default true
			@Setting("type") ArgType type,
			@Setting("min") Long min,
			@Setting("max") Long max,
			@Setting("choices") List<String> choices,
			@Setting("pattern") String pattern,
			@Setting("rest") Boolean rest) {
	}

	@ConfigSerializable
	public static record CommandStep(
			@Setting("command") String command,
			@Setting("run-as") ExecutorMode runAs,
			@Setting("target") TargetSpec target,
			@Setting("delay") String delay,
			@Setting("timeout") String timeout) {
	}
}
