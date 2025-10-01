package dev.objz.commandbridge.main.scripting.v3.model.domain;

import java.time.Duration;

public record TargetServer(
		boolean targetRequired,
		boolean scheduleOnline,
		Duration timeout,
		Duration frequency) {
}
