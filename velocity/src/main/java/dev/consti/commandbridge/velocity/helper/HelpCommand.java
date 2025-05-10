package dev.consti.commandbridge.velocity.helper;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.consti.foundationlib.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class HelpCommand {
    public static LiteralArgumentBuilder<CommandSource> build(Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("help")
                .executes(context -> sendHelpMessage(context.getSource(), logger));
    }

    public static int sendHelpMessage(CommandSource source, Logger logger) {
        logger.debug("Sending help message to: {}", source);

        source.sendMessage(Component.text("==== CommandBridge Help ====").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text(""));

        source.sendMessage(Component.text("Available Commands:").color(NamedTextColor.YELLOW));

        source.sendMessage(formatCommand("help", "Displays this help message", false));
        source.sendMessage(formatCommand("list", "Lists connected clients", false));
        source.sendMessage(formatCommand("reload", "Reloads all configs and scripts", false));
        source.sendMessage(formatCommand("version", "Displays the current version", false));

        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("Debug Commands (Debug mode only):").color(NamedTextColor.RED));
        source.sendMessage(formatCommand("stop", "Stops the cb server", false));
        source.sendMessage(formatCommand("start", "Starts the cb server", false));

        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("Client Commands (/cbc):").color(NamedTextColor.AQUA));
        source.sendMessage(formatCommand("reconnect", "Reconnects the current client", true));

        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("Website: ")
                .append(Component.text("https://cb.objz.dev")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl("https://cb.objz.dev"))));

        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));
        return 1;
    }

    private static Component formatCommand(String command, String description, boolean client) {
        String alias = client ? "/cbc " : "/cb ";
        return Component.text("  - ")
                .append(Component.text(alias + command).color(NamedTextColor.GREEN))
                .append(Component.text(" - " + description).color(NamedTextColor.WHITE));
    }
}
