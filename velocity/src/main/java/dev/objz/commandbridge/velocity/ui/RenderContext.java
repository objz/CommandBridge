package dev.objz.commandbridge.velocity.ui;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public final class RenderContext {
    private final CommandSource source;
    private final boolean isPlayer;
    private final int width;

    public RenderContext(CommandSource source) {
        this.source = source;
        this.isPlayer = source instanceof Player;
        this.width = isPlayer ? 50 : 100;
    }

    public CommandSource source() {
        return source;
    }

    public boolean isPlayer() {
        return isPlayer;
    }

    public boolean isConsole() {
        return !isPlayer;
    }

    public int width() {
        return width;
    }

    public void send(Component component) {
        if (isPlayer) {
            source.sendMessage(component);
        }
    }
}
