package dev.objz.commandbridge.velocity.dispatch;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import net.kyori.adventure.text.Component;

/** Permission assertions are accepted only from the authenticated proxy tracking this player. */
record RemotePlayerSource(SenderContext.Player player, String proxyId) implements CommandSource {

    @Override
    public boolean hasPermission(String permission) {
        return player.permissions().contains(permission);
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        return hasPermission(permission) ? Tristate.TRUE : Tristate.FALSE;
    }

    @Override
    public void sendMessage(Component message) {
        Log.warn("Remote player '{}' on '{}': {}", player.uuid(), proxyId, message);
    }
}
