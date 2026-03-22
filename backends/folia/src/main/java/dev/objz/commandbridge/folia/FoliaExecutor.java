package dev.objz.commandbridge.folia;

import dev.objz.commandbridge.backends.platform.PlatformExecutor;
import dev.objz.commandbridge.logging.Log;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class FoliaExecutor extends PlatformExecutor {

    public FoliaExecutor(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected CompletableFuture<ExecutionResult> dispatchCommandWithPermissions(
            Player player, String command, Set<String> permissions) {
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

        player.getScheduler().execute(plugin, () -> {
            Player current = Bukkit.getPlayer(player.getUniqueId());
            if (current == null || !current.isOnline()) {
                future.complete(ExecutionResult.playerOffline(player.getUniqueId()));
                return;
            }

            PermissionAttachment attachment = null;
            try {
                attachment = current.addAttachment(plugin);
                for (String perm : permissions) {
                    attachment.setPermission(perm, true);
                }

                boolean success = Bukkit.dispatchCommand(current, command);
                future.complete(success ? ExecutionResult.success()
                        : ExecutionResult.failure("Command returned false"));
            } catch (Exception e) {
                Log.error(e, "Exception while executing command '{}' as operator", command);
                future.complete(ExecutionResult.failure(e.getMessage()));
            } finally {
                if (attachment != null) {
                    try {
                        current.removeAttachment(attachment);
                    } catch (Exception e) {
                        Log.warn("Failed to remove permission attachment: {}", e.getMessage());
                    }
                }
            }
        }, () -> future.complete(ExecutionResult.playerOffline(player.getUniqueId())), 0L);

        return future;
    }

    @Override
    protected CompletableFuture<ExecutionResult> dispatchCommand(CommandSender sender, String command) {
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

        if (sender instanceof Player player) {
            player.getScheduler().execute(plugin, () -> {
                try {
                    boolean success = Bukkit.dispatchCommand(sender, command);
                    future.complete(success ? ExecutionResult.success()
                            : ExecutionResult.failure("Command returned false"));
                } catch (Exception e) {
                    Log.error(e, "Exception while executing player command '{}'", command);
                    future.complete(ExecutionResult.failure(e.getMessage()));
                }
            }, () -> future.complete(ExecutionResult.playerOffline(player.getUniqueId())), 0L);
        } else {
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                try {
                    boolean success = Bukkit.dispatchCommand(sender, command);
                    future.complete(success ? ExecutionResult.success()
                            : ExecutionResult.failure("Command returned false"));
                } catch (Exception e) {
                    Log.error(e, "Exception while executing global command '{}'", command);
                    future.complete(ExecutionResult.failure(e.getMessage()));
                }
            });
        }

        return future;
    }
}
