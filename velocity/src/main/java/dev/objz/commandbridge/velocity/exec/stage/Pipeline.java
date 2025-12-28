package dev.objz.commandbridge.velocity.exec.stage;

import dev.objz.commandbridge.velocity.exec.ExecutionContext;
import dev.objz.commandbridge.velocity.exec.ExecutionResult;

public interface Pipeline {
	ExecutionResult process(ExecutionContext context);

	default int order() {
		return 100;
	}
}
