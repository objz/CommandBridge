package dev.objz.commandbridge.bukkit;

import dev.objz.commandbridge.backends.platform.PlatformExecutor;
import dev.objz.commandbridge.logging.Log;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class BukkitExecutor extends PlatformExecutor {

	public BukkitExecutor(JavaPlugin plugin) {
		super(plugin);
	}

	@Override
	protected CompletableFuture<ExecutionResult> dispatchCommand(CommandSender sender, String command) {
		CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

		if (Bukkit.isPrimaryThread()) {
			executeNow(sender, command, future);
		} else {
			Bukkit.getScheduler().runTask(plugin, () -> executeNow(sender, command, future));
		}

		return future;
	}

	private void executeNow(CommandSender sender, String command, CompletableFuture<ExecutionResult> future) {
		try {
			boolean success = Bukkit.dispatchCommand(sender, command);
			if (success) {
				future.complete(ExecutionResult.success());
			} else {
				future.complete(ExecutionResult.failure("Command returned false"));
			}
		} catch (Exception e) {
			Log.error(e, "Exception while executing command '{}'", command);
			future.complete(ExecutionResult.failure(e.getMessage()));
		}
	}
}
