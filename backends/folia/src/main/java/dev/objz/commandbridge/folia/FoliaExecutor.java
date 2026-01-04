package dev.objz.commandbridge.folia;

import dev.objz.commandbridge.backends.platform.PlatformExecutor;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Folia command executor using regionized schedulers.
 * Commands targeting players are scheduled on the player's region.
 * Console commands are scheduled on the global region.
 */
public final class FoliaExecutor extends PlatformExecutor {

	public FoliaExecutor(JavaPlugin plugin) {
		super(plugin);
	}

	@Override
	public CompletableFuture<ExecutionResult> execute(ExecuteCommand command) {
		String cmd = command.command();
		if (cmd.startsWith("/")) {
			cmd = cmd.substring(1);
		}

		RunAs runAs = command.runAs();
		if (runAs == null) {
			runAs = RunAs.CONSOLE;
		}

		UUID playerUuid = command.uuid();
		final String finalCmd = cmd;
		final RunAs finalRunAs = runAs;

		// For Folia, we need to handle scheduling differently based on whether
		// we're targeting a player or console
		if (playerUuid != null && (finalRunAs == RunAs.PLAYER || finalRunAs == RunAs.OPERATOR)) {
			return executeForPlayer(playerUuid, finalCmd, finalRunAs);
		} else {
			return executeGlobal(finalCmd);
		}
	}

	/**
	 * Execute a command in the global region (for console commands).
	 */
	private CompletableFuture<ExecutionResult> executeGlobal(String command) {
		CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

		GlobalRegionScheduler globalScheduler = Bukkit.getGlobalRegionScheduler();
		globalScheduler.execute(plugin, () -> {
			try {
				CommandSender console = Bukkit.getConsoleSender();
				boolean success = Bukkit.dispatchCommand(console, command);
				if (success) {
					future.complete(ExecutionResult.success());
				} else {
					future.complete(ExecutionResult.failure("Command returned false"));
				}
			} catch (Exception e) {
				Log.error(e, "Exception while executing global command '{}'", command);
				future.complete(ExecutionResult.failure(e.getMessage()));
			}
		});

		return future;
	}

	/**
	 * Execute a command in the player's region.
	 */
	private CompletableFuture<ExecutionResult> executeForPlayer(UUID playerUuid, String command, RunAs runAs) {
		CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

		Player player = Bukkit.getPlayer(playerUuid);
		if (player == null || !player.isOnline()) {
			future.complete(ExecutionResult.playerNotFound(playerUuid));
			return future;
		}

		EntityScheduler entityScheduler = player.getScheduler();
		entityScheduler.execute(plugin, () -> {
			// Re-check player is still online after scheduling
			Player currentPlayer = Bukkit.getPlayer(playerUuid);
			if (currentPlayer == null || !currentPlayer.isOnline()) {
				future.complete(ExecutionResult.playerOffline(playerUuid));
				return;
			}

			try {
				CommandSender sender;
				if (runAs == RunAs.PLAYER || runAs == RunAs.OPERATOR) {
					sender = currentPlayer;
				} else {
					sender = Bukkit.getConsoleSender();
				}

				boolean success = Bukkit.dispatchCommand(sender, command);
				if (success) {
					future.complete(ExecutionResult.success());
				} else {
					future.complete(ExecutionResult.failure("Command returned false"));
				}
			} catch (Exception e) {
				Log.error(e, "Exception while executing player command '{}'", command);
				future.complete(ExecutionResult.failure(e.getMessage()));
			}
		}, () -> {
			// Retired callback - player went offline or was removed
			future.complete(ExecutionResult.playerOffline(playerUuid));
		}, 0L);

		return future;
	}

	@Override
	protected CompletableFuture<ExecutionResult> dispatchCommand(CommandSender sender, String command) {
		// This method is not used directly in Folia because we override execute()
		// But we provide an implementation for completeness
		CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

		if (sender instanceof Player player) {
			player.getScheduler().execute(plugin, () -> {
				try {
					boolean success = Bukkit.dispatchCommand(sender, command);
					future.complete(success ? ExecutionResult.success()
							: ExecutionResult.failure("Command returned false"));
				} catch (Exception e) {
					future.complete(ExecutionResult.failure(e.getMessage()));
				}
			}, () -> future.complete(ExecutionResult.failure("Player retired")), 0L);
		} else {
			Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
				try {
					boolean success = Bukkit.dispatchCommand(sender, command);
					future.complete(success ? ExecutionResult.success()
							: ExecutionResult.failure("Command returned false"));
				} catch (Exception e) {
					future.complete(ExecutionResult.failure(e.getMessage()));
				}
			});
		}

		return future;
	}
}
