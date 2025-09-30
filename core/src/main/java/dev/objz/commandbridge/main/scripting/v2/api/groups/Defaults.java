package dev.objz.commandbridge.main.scripting.v3.api.groups;

import java.time.Duration;

import dev.objz.commandbridge.main.scripting.v3.api.constants.TargetConstants;
import dev.objz.commandbridge.main.scripting.v3.api.groups.defaults.Target;

public record Defaults(
		Target target,
		Duration delay,
		Duration cooldown) {

	public static Defaults withDefaults() {
		return new Defaults(Target.withDefaults(), TargetConstants.DELAY, TargetConstants.COOLDOWN);
	}
}
