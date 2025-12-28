package dev.objz.commandbridge.velocity.exec;

public sealed interface ExecutionResult permits ExecutionResult.Next, ExecutionResult.Stop {

	record Next(ExecutionContext context) implements ExecutionResult {
	}

	record Stop(String reason, boolean isError) implements ExecutionResult {
	}

	static ExecutionResult next(ExecutionContext ctx) {
		return new Next(ctx);
	}

	static ExecutionResult stop(String reason) {
		return new Stop(reason, false);
	}

	static ExecutionResult error(String reason) {
		return new Stop(reason, true);
	}
}
