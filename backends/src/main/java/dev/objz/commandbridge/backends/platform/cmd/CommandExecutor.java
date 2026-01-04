package dev.objz.commandbridge.backends.platform.cmd;

import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CommandExecutor {

	/**
	 * @param command The command execution context containing command string, runAs
	 *                mode, and optional player UUID
	 * @return CompletableFuture that completes when execution is done (or
	 *         scheduled)
	 */
	CompletableFuture<ExecutionResult> execute(ExecuteCommand command);

	/**
	 * @param command            The command execution context
	 * @param grantedPermissions Set of permissions to temporarily grant
	 * @return CompletableFuture that completes when execution is done
	 */
	CompletableFuture<ExecutionResult> execute(ExecuteCommand command, Set<String> grantedPermissions);

	record ExecutionResult(
			boolean successful,
			String message,
			UUID playerUuid) {

		public boolean isSuccess() {
			return successful;
		}

		public static ExecutionResult success() {
			return new ExecutionResult(true, null, null);
		}

		public static ExecutionResult success(String message) {
			return new ExecutionResult(true, message, null);
		}

		public static ExecutionResult failure(String message) {
			return new ExecutionResult(false, message, null);
		}

		public static ExecutionResult playerNotFound(UUID uuid) {
			return new ExecutionResult(false, "Player not found: " + uuid, uuid);
		}

		public static ExecutionResult playerOffline(UUID uuid) {
			return new ExecutionResult(false, "Player is offline: " + uuid, uuid);
		}
	}
}
