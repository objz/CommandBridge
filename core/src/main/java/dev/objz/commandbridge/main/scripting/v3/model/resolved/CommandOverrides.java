package dev.objz.commandbridge.main.scripting.v3.model.resolved;

import java.time.Duration;

public record CommandOverrides(
		Target target, // null means inherit
		Duration delay, // null means inherit
		Duration timeout // null means inherit
) {
}
