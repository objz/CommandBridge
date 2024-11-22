package dev.consti.velocity.utils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.logging.Logger;
import dev.consti.utils.VersionChecker;
import dev.consti.velocity.Main;
import dev.consti.velocity.core.Runtime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.function.Consumer;

public class GeneralUtils {
    private final Logger logger;
    private final ProxyServer proxy;
    private final Main plugin;

    public GeneralUtils(Logger logger) {
        this.logger = logger;
        this.proxy = ProxyUtils.getProxyServer();
        this.plugin = Main.getInstance();
    }

    public void registerCommands() {
        logger.info("Registering commands for CommandBridge...");
        try {
            LiteralCommandNode<CommandSource> commandBridgeNode = LiteralArgumentBuilder.<CommandSource>literal("commandbridge")
                    .executes(context -> {
                        if (context.getSource().hasPermission("commandbridge.admin")) {
                            logger.debug("Help command executed by: {}", context.getSource());
                            return sendHelpMessage(context);
                        }
                        context.getSource().sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                        return 0;
                    })
                    .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                            .executes(context -> {
                                if (context.getSource().hasPermission("commandbridge.admin")) {
                                    Runtime.getInstance().getConfig().reload();
                                    Runtime.getInstance().getScriptUtils().reload();
                                    // TODO: Send message to Bukkit and reload the scripts and config there also
                                    context.getSource().sendMessage(Component.text("Scripts reloaded!", NamedTextColor.GREEN));
                                    logger.info("Scripts reloaded by: {}", context.getSource());
                                    return 1;
                                }
                                context.getSource().sendMessage(Component.text("You do not have permission to reload scripts.", NamedTextColor.RED));
                                return 0;
                            })
                            .build())
                    .then(LiteralArgumentBuilder.<CommandSource>literal("version")
                            .executes(context -> {
                                if (context.getSource().hasPermission("commandbridge.admin")) {
                                    CommandSource source = context.getSource();
                                    String currentVersion = Main.getVersion();

                                    source.sendMessage(Component.text("Checking for updates...").color(NamedTextColor.YELLOW));
                                    logger.debug("Version command executed by: {}", source);

                                    new Thread(() -> {
                                        String latestVersion = VersionChecker.getLatestVersion();

                                        if (latestVersion == null) {
                                            source.sendMessage(Component.text("Unable to check for updates.").color(NamedTextColor.RED));
                                            logger.warn("Failed to retrieve latest version for update check.");
                                            return;
                                        }

                                        logger.debug("Current version: {}, Latest version: {}", currentVersion, latestVersion);

                                        if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                                            source.sendMessage(Component.text("A new version is available: " + latestVersion).color(NamedTextColor.RED));
                                            source.sendMessage(Component.text("Please download the latest release: ")
                                                    .append(Component.text("here")
                                                            .color(NamedTextColor.BLUE)
                                                            .decorate(TextDecoration.UNDERLINED)
                                                            .clickEvent(ClickEvent.openUrl(VersionChecker.getDownloadUrl()))));
                                            logger.warn("A newer version is available: {}", latestVersion);
                                        } else {
                                            source.sendMessage(Component.text("You are running the latest version: " + currentVersion).color(NamedTextColor.GREEN));
                                            logger.info("Latest version is already installed: {}", currentVersion);
                                        }
                                    }).start();

                                    return 1;
                                }
                                context.getSource().sendMessage(Component.text("You do not have permission to check the version.", NamedTextColor.RED));
                                return 0;
                            })
                            .build())
                    .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                            .executes(this::sendHelpMessage)
                            .build())
                    .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                            .executes(this::listServers)
                            .build())
                    .build();

            CommandMeta commandMeta = proxy.getCommandManager()
                    .metaBuilder("commandbridge")
                    .aliases("cb")
                    .plugin(plugin)
                    .build();

            BrigadierCommand brigadierCommand = new BrigadierCommand(commandBridgeNode);
            proxy.getCommandManager().register(commandMeta, brigadierCommand);
            logger.info("CommandBridge commands registered successfully.");
        } catch (Exception e) {
            logger.error("Failed to register CommandBridge commands: {}", e.getMessage(), e);
        }
    }

    private int listServers(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        List<String> connectedClients = Runtime.getInstance().getServer().getConnectedClients();

        if (connectedClients.isEmpty()) {
            source.sendMessage(Component.text("No clients are currently connected.").color(NamedTextColor.RED));
        } else {
            // Convert the list of clients to a single string, separated by commas
            String clientsString = String.join(", ", connectedClients);

            // Send the message with the connected clients
            source.sendMessage(Component.text("===== Connected Clients =====").color(NamedTextColor.GOLD));
            source.sendMessage(Component.text(clientsString).color(NamedTextColor.GREEN));
            source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));
        }

        return 1;
    }



    private int sendHelpMessage(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        logger.debug("Sending help message to: {}", source);

        source.sendMessage(Component.text("===== CommandBridge Help =====").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text(""));

        source.sendMessage(Component.text("Commands:").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  - ").append(Component.text("/commandbridge reload").color(NamedTextColor.GREEN))
                .append(Component.text(" - Reloads scripts").color(NamedTextColor.WHITE)));
        source.sendMessage(Component.text("  - ").append(Component.text("/commandbridge version").color(NamedTextColor.GREEN))
                .append(Component.text(" - Displays the plugin version").color(NamedTextColor.WHITE)));
        source.sendMessage(Component.text("  - ").append(Component.text("/commandbridge help").color(NamedTextColor.GREEN))
                .append(Component.text(" - Displays this help message").color(NamedTextColor.WHITE)));
        source.sendMessage(Component.text(""));

        source.sendMessage(Component.text("Detailed Documentation: ")
                .append(Component.text("https://72-s.github.io/CommandBridge/")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl("https://72-s.github.io/CommandBridge/"))));

        source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));
        return 1;
    }
}
