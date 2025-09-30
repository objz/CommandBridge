package dev.objz.commandbridge.main.scripting.v3.model.resolved;

import java.time.Duration;

public record Defaults(
		Target target,
		Duration delay,
		Duration cooldown) {
}
