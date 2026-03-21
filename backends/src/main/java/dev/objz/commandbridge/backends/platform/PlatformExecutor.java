package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class PlatformExecutor implements CommandExecutor {

    protected final JavaPlugin plugin;

    protected PlatformExecutor(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecuteCommand command) {
        return execute(command, null);
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(ExecuteCommand command, Set<String> grantedPermissions) {
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

                    if (finalRunAs == RunAs.OPERATOR && sender instanceof Player player
                            && grantedPermissions != null) {
                        return dispatchCommandWithPermissions(player, finalCmd,
                                grantedPermissions);
                    }

                    return dispatchCommand(sender, finalCmd);
                });
    }

    protected CompletableFuture<ExecutionResult> dispatchCommandWithPermissions(
            Player player, String command, Set<String> permissions) {
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

        Runnable task = () -> {
            PermissionAttachment attachment = null;
            try {
                attachment = player.addAttachment(plugin);
                for (String perm : permissions) {
                    attachment.setPermission(perm, true);
                }
                String baseCommand = command.split(" ")[0];
                attachment.setPermission(baseCommand, true);
                attachment.setPermission("*", true);

                boolean success = Bukkit.dispatchCommand(player, command);
                if (success) {
                    future.complete(ExecutionResult.success());
                } else {
                    future.complete(ExecutionResult.failure("Command returned false"));
                }
            } catch (Exception e) {
                Log.error(e, "Exception while executing command '{}' as operator", command);
                future.complete(ExecutionResult.failure(e.getMessage()));
            } finally {
                if (attachment != null) {
                    try {
                        player.removeAttachment(attachment);
                    } catch (Exception e) {
                        Log.warn("Failed to remove permission attachment: {}", e.getMessage());
                    }
                }
            }
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }

        return future;
    }

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
     * @param sender  The command sender
     * @param command The command to execute
     * @return CompletableFuture that completes when the command has been dispatched
     */
    protected abstract CompletableFuture<ExecutionResult> dispatchCommand(CommandSender sender, String command);
}
