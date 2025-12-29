package dev.objz.commandbridge.velocity.exec;

public sealed interface ExecutionResult {

	record Continue(ExecutionContext context) implements ExecutionResult {
	}

	record Stop(String reason) implements ExecutionResult {
	}

	record Error(String message) implements ExecutionResult {
	}

	static ExecutionResult ok(ExecutionContext ctx) {
		return new Continue(ctx);
	}

	static ExecutionResult stop(String reason) {
		return new Stop(reason);
	}

	static ExecutionResult error(String message) {
		return new Error(message);
	}
}
