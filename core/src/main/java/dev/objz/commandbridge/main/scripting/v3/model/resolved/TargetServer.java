package dev.objz.commandbridge.main.scripting.v3.model.resolved;

import java.time.Duration;

public record TargetServer(
		boolean targetRequired,
		boolean scheduleOnline,
		Duration timeout,
		Duration frequency) {
}
