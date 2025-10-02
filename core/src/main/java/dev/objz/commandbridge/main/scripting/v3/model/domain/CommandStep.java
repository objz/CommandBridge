package dev.objz.commandbridge.main.scripting.v3.model.domain;

import java.time.Duration;

public record CommandStep(
		String command,
		TargetKind kindOverride,
		TargetServer serverOverride,
		Duration delayOverride,
		Duration timeoutOverride) {
}
