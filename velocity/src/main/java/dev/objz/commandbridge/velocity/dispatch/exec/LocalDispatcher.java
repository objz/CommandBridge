package dev.objz.commandbridge.velocity.dispatch.exec;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.api.channel.command.RunAs;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LocalDispatcher {

    private final ProxyServer proxy;
    private final String localServerId;

    public LocalDispatcher(ProxyServer proxy, String localServerId) {
        this.proxy = Objects.requireNonNull(proxy);
        this.localServerId = Objects.requireNonNull(localServerId);
    }

    public String getLocalServerId() {
        return localServerId;
    }

    public boolean isLocal(String targetId) {
        return localServerId.equals(targetId);
    }

    public CompletableFuture<Boolean> execute(String command, RunAs runAs, UUID playerUuid,
            CommandSource fallbackSource) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;

        CommandSource sender = switch (runAs != null ? runAs : RunAs.CONSOLE) {
            case CONSOLE -> proxy.getConsoleCommandSource();
            case PLAYER -> {
                if (playerUuid != null) {
                    yield proxy.getPlayer(playerUuid).orElse(null);
                }
                yield fallbackSource instanceof Player p ? p : null;
            }
            case OPERATOR -> {
                if (playerUuid != null) {
                    Player player = proxy.getPlayer(playerUuid).orElse(null);
                    if (player != null) {
                        yield proxy.getConsoleCommandSource();
                    }
                }
                yield proxy.getConsoleCommandSource();
            }
        };

        if (sender == null) {
            Log.warn("Could not resolve sender for command '{}'", cmd);
            return CompletableFuture.completedFuture(false);
        }

        Log.debug("Executing local Velocity command '{}' as {}", cmd,
                sender instanceof Player p ? p.getUsername() : "CONSOLE");

        return proxy.getCommandManager().executeAsync(sender, cmd)
                .exceptionally(ex -> {
                    Log.error(ex, "Velocity command '{}' threw exception", cmd);
                    return false;
                });
    }
}
