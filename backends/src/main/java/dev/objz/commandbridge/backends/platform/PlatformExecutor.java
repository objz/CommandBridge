package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Base command executor with common logic for all Bukkit-based platforms.
 * Subclasses implement platform-specific scheduling.
 */
public abstract class PlatformExecutor implements CommandExecutor {

	protected final JavaPlugin plugin;

	protected PlatformExecutor(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
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

		return resolveExecutor(finalRunAs, playerUuid)
				.thenCompose(sender -> {
					if (sender == null) {
						if (playerUuid != null) {
							return CompletableFuture.completedFuture(
									ExecutionResult.playerNotFound(playerUuid));
						}
						return CompletableFuture.completedFuture(
								ExecutionResult.failure(
										"Could not resolve command sender"));
					}
					return dispatchCommand(sender, finalCmd);
				});
	}

	/**
	 * Resolve the command sender based on RunAs mode and optional player UUID.
	 */
	protected CompletableFuture<CommandSender> resolveExecutor(RunAs runAs, UUID playerUuid) {
		return CompletableFuture.supplyAsync(() -> {
			switch (runAs) {
				case CONSOLE:
					return Bukkit.getConsoleSender();

				case PLAYER:
					if (playerUuid == null) {
						Log.warn("RunAs. PLAYER requires a player UUID but none provided");
						return null;
					}
					Player player = Bukkit.getPlayer(playerUuid);
					if (player == null || !player.isOnline()) {
						Log.warn("Player with UUID {} is not online", playerUuid);
						return null;
					}
					return player;

				case OPERATOR:
					if (playerUuid == null) {
						Log.warn("RunAs.OPERATOR requires a player UUID but none provided, falling back to console");
						return Bukkit.getConsoleSender();
					}
					Player opPlayer = Bukkit.getPlayer(playerUuid);
					if (opPlayer == null || !opPlayer.isOnline()) {
						Log.warn("Player with UUID {} is not online for OPERATOR execution",
								playerUuid);
						return null;
					}
					return opPlayer;

				default:
					Log.warn("Unknown RunAs mode: {}, defaulting to CONSOLE", runAs);
					return Bukkit.getConsoleSender();
			}
		});
	}

	/**
	 * Dispatch the command on the main thread using platform-specific scheduling.
	 * Subclasses must implement this with their platform's scheduler.
	 *
	 * @param sender  The command sender (console or player)
	 * @param command The command to execute (without leading slash)
	 * @return CompletableFuture that completes when the command has been dispatched
	 */
	protected abstract CompletableFuture<ExecutionResult> dispatchCommand(CommandSender sender, String command);
}
