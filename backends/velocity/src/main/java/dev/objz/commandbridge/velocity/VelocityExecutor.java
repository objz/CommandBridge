package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class VelocityExecutor implements CommandExecutor {

    private final ProxyServer proxy;
    private final Object plugin;

    public VelocityExecutor(ProxyServer proxy, Object plugin) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
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
        CommandSource source = resolveSource(runAs, playerUuid);

        if (source == null) {
            if (playerUuid != null && (runAs == RunAs.PLAYER || runAs == RunAs.OPERATOR)) {
                return CompletableFuture.completedFuture(ExecutionResult.playerOffline(playerUuid));
            }
            return CompletableFuture
                    .completedFuture(ExecutionResult.failure("Could not resolve command source"));
        }

        if (runAs == RunAs.OPERATOR && grantedPermissions != null && !grantedPermissions.isEmpty()) {
            source = new PermissibleCommandSource(source, grantedPermissions);
        }

        final String finalCmd = cmd;
        return proxy.getCommandManager().executeAsync(source, cmd)
                .handle((success, ex) -> {
                    if (ex != null) {
                        Log.error(ex, "Error executing Velocity command '{}'", finalCmd);
                        return ExecutionResult.failure(ex.getMessage());
                    }
                    if (Boolean.TRUE.equals(success)) {
                        return ExecutionResult.success();
                    } else {
                        return ExecutionResult.failure("Command returned false");
                    }
                });
    }

    private CommandSource resolveSource(RunAs runAs, UUID playerUuid) {
        switch (runAs) {
            case CONSOLE:
                return proxy.getConsoleCommandSource();
            case PLAYER:
                if (playerUuid == null)
                    return null;
                return proxy.getPlayer(playerUuid).orElse(null);
            case OPERATOR:
                if (playerUuid == null)
                    return proxy.getConsoleCommandSource();
                Player p = proxy.getPlayer(playerUuid).orElse(null);
                return p != null ? p : proxy.getConsoleCommandSource();
            default:
                return proxy.getConsoleCommandSource();
        }
    }

    private static class PermissibleCommandSource implements CommandSource {
        private final CommandSource delegate;
        private final Set<String> permissions;

        public PermissibleCommandSource(CommandSource delegate, Set<String> permissions) {
            this.delegate = delegate;
            this.permissions = permissions;
        }

        @Override
        public void sendMessage(Component component) {
            delegate.sendMessage(component);
        }

        @Override
        public Tristate getPermissionValue(String permission) {
            if (permissions.contains(permission) || permissions.contains("*")) {
                return Tristate.TRUE;
            }
            return delegate.getPermissionValue(permission);
        }

        @Override
        public boolean hasPermission(String permission) {
            return getPermissionValue(permission) == Tristate.TRUE;
        }
    }
}
