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

        source.sendMessage(Component.text("===== CommandBridge HelpCommand =====").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("Commands:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  - ")
                .append(Component.text("/commandbridge reload").color(NamedTextColor.GREEN))
                .append(Component.text(" - Reloads scripts").color(NamedTextColor.WHITE)));
        source.sendMessage(Component.text("  - ")
                .append(Component.text("/commandbridge version").color(NamedTextColor.GREEN))
                .append(Component.text(" - Displays the plugin version").color(NamedTextColor.WHITE)));
        source.sendMessage(Component.text("  - ")
                .append(Component.text("/commandbridge help").color(NamedTextColor.GREEN))
                .append(Component.text(" - Displays this help message").color(NamedTextColor.WHITE)));
        source.sendMessage(Component.text(""));
        source.sendMessage(
                Component.text("Detailed Documentation: ")
                        .append(
                                Component.text("https://72-s.github.io/CommandBridge/")
                                        .color(NamedTextColor.LIGHT_PURPLE)
                                        .decorate(TextDecoration.UNDERLINED)
                                        .clickEvent(ClickEvent.openUrl("https://72-s.github.io/CommandBridge/"))));
        source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));
        return 1;
    }
}

