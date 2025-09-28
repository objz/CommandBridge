package dev.objz.commandbridge.main.scripting.v3.api.groups.defaults;

import java.time.Duration;

import dev.objz.commandbridge.main.scripting.v3.api.Meta;
import dev.objz.commandbridge.main.scripting.v3.api.constants.TargetConstants;

public record TargetServer(
		Meta<Boolean> targetRequired,
		Meta<Boolean> scheduleOnline,
		Meta<Duration> timeout,
		Meta<Duration> frequency) {

	public static TargetServer withDefaults() {
		return new TargetServer(
				Meta.isOptional(TargetConstants.REQUIRED),
				Meta.isOptional(TargetConstants.SCHEDULE),
				Meta.isOptional(TargetConstants.TIMEOUT),
				Meta.isOptional(TargetConstants.FREQUENCY));
	}
}
