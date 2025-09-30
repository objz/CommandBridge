package dev.objz.commandbridge.main.scripting.v3.api.groups.commands;

import java.time.Duration;

import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.Target;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.TargetKind;

public record ResolvedStep(
		String command,
		Target.RunAs runAs,
		String id,
		TargetKind.Type register,
		TargetKind.Type execute,
		boolean targetRequired,
		boolean scheduleOnline,
		Duration timeout,
		Duration frequency,
		Duration delay,
		Duration cooldown) {
}
