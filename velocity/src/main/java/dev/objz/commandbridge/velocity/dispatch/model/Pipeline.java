package dev.objz.commandbridge.velocity.dispatch.model;

import java.util.function.Consumer;

public interface Pipeline {
	/**
	 * 
	 * @param context The current execution context
	 * @param next    The consumer to call when this stage is done (if successful)
	 */
	void process(ExecutionContext context, Consumer<ExecutionResult> next);
}
