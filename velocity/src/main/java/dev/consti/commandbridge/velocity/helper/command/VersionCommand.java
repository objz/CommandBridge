package dev.consti.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.consti.commandbridge.velocity.Main;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.VersionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;


public class VersionCommand {
    public static LiteralArgumentBuilder<CommandSource> build(Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("version")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (!source.hasPermission("commandbridge.admin")) {
                        source.sendMessage(Component.text("You do not have permission to check the version", NamedTextColor.RED));
                        return 0;
                    }

                    String currentVersion = Main.getVersion();
                    source.sendMessage(Component.text("Checking for updates...").color(NamedTextColor.YELLOW));
                    logger.debug("VersionCommand executed by: {}", source);

                    new Thread(() -> {
                        String latestVersion = VersionChecker.getLatestVersion();

                        if (latestVersion == null) {
                            source.sendMessage(
                                    Component.text("Unable to check for updates").color(NamedTextColor.RED));
                            logger.warn("Failed to retrieve latest version for update check");
                            return;
                        }

                        logger.debug("Current version: {}, Latest version: {}", currentVersion, latestVersion);

                        if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                            source.sendMessage(
                                    Component.text("A new version is available: " + latestVersion)
                                            .color(NamedTextColor.RED));
                            source.sendMessage(
                                    Component.text("Please download the latest release: ")
                                            .append(
                                                    Component.text("here")
                                                            .color(NamedTextColor.BLUE)
                                                            .decorate(TextDecoration.UNDERLINED)
                                                            .clickEvent(ClickEvent.openUrl(VersionChecker.getDownloadUrl()))));
                            logger.warn("A newer version is available: {}", latestVersion);
                        } else {
                            source.sendMessage(
                                    Component.text("You are running the latest version: " + currentVersion)
                                            .color(NamedTextColor.GREEN));
                        }
                    }).start();

                    return 1;
                });
    }
}

