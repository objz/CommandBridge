package org.commandbridge.runtime;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;
import org.commandbridge.utilities.VersionChecker;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class Startup {

    private final ProxyServer server;
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;
    private boolean verboseOutput;
    private final VelocityRuntime velocityRuntime;
    private final Path dataDirectory = Path.of("plugins", "CommandBridgeVelocity");

    public Startup(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
        this.velocityRuntime = new VelocityRuntime(server, plugin);
    }

    public void loadConfig() {
        File dataFolder = new File("plugins/CommandBridgeVelocity");
        if (!dataFolder.exists()) {
            boolean dirsCreated = dataFolder.mkdirs();
            if (!dirsCreated) {
                verboseLogger.warn("Failed to create the directory: " + dataFolder.getPath());
                return; // Stop execution if the directory couldn't be created
            }
        }

        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            try {
                Files.createFile(configFile.toPath());
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("verbose-output: false\n");
                }
                verboseLogger.info("Config file created with default settings.");
            } catch (IOException e) {
                verboseLogger.error("Failed to create the default config file", e);
            }
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(new Constructor(loaderOptions));
            Map<String, Object> data = yaml.load(fis);
            verboseOutput = (boolean) data.getOrDefault("verbose-output", false);
            verboseLogger.forceInfo("Config loaded. Verbose output is " + (verboseOutput ? "enabled" : "disabled"));
            velocityRuntime.loadScripts();
            copyExampleYml();
        } catch (IOException e) {
            verboseLogger.error("Failed to load config file", e);
        } catch (ClassCastException e) {
            verboseLogger.error("Invalid data type in config file", e);
        } catch (Exception e) {
            verboseLogger.error("Unexpected error while loading config file", e);
        }

        // Perform version check on startup
        checkForUpdatesOnStartup();
    }

    public boolean isVerboseOutput() {
        return verboseOutput;
    }


    private void copyExampleYml() {
        Path scriptFolder = dataDirectory.resolve("scripts");
        try {
            Files.createDirectories(scriptFolder);
            Path exampleScript = scriptFolder.resolve("example.yml");

            if (Files.notExists(exampleScript)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("example.yml")) {
                    if (in == null) {
                        verboseLogger.warn("Could not find example.yml in resources");
                        return;
                    }
                    Files.copy(in, exampleScript, StandardCopyOption.COPY_ATTRIBUTES);
                    verboseLogger.info("example.yml has been copied successfully.");
                }
            } else {
                verboseLogger.info("example.yml already exists at " + exampleScript);
            }
        } catch (IOException e) {
            verboseLogger.error("Failed to create scripts folder or copy example.yml", e);
        }
    }

    private void checkForUpdatesOnStartup() {
        String currentVersion = plugin.getVersion();
        verboseLogger.info("Checking for updates...");

        new Thread(() -> {
            String latestVersion = VersionChecker.getLatestVersion();

            if (latestVersion == null) {
                verboseLogger.warn("Unable to check for updates.");
                return;
            }

            if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                verboseLogger.warn("A new version is available: " + latestVersion);
                verboseLogger.warn("Please download the latest release: https://modrinth.com/plugin/wIuI4ru2");
            } else {
                verboseLogger.info("You are running the latest version: " + currentVersion);
            }
        }).start();
    }

    private void checkForBukkitVersion() {
        plugin.getMessageSender().sendSystemCommand("version");
    }

    public void isSameBukkitVersion(boolean version) {
        if (version) {
            verboseLogger.info("All Bukkit servers are running on the right version");
        } else {
            verboseLogger.warn("Please update all Bukkit servers to the version: CommandBridgeBukkit-" + plugin.getVersion());
        }
    }

    public void registerCommands() {
        LiteralCommandNode<CommandSource> commandBridgeNode = LiteralArgumentBuilder.<CommandSource>literal("commandbridge")
                .executes(context -> {
                    if (context.getSource().hasPermission("commandbridge.admin")) {
                        context.getSource().sendMessage(Component.text("Use /commandbridge reload to reload scripts."));
                        return 1;
                    }
                    context.getSource().sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                    return 0;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .executes(context -> {
                            if (context.getSource().hasPermission("commandbridge.admin")) {
                                plugin.getStartup().loadConfig();
                                plugin.getMessageSender().sendSystemCommand("reload");
                                context.getSource().sendMessage(Component.text("Scripts reloaded!", NamedTextColor.GREEN));
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
                                String currentVersion = plugin.getVersion();

                                source.sendMessage(Component.text("Checking for updates...").color(NamedTextColor.YELLOW));
                                checkForBukkitVersion();

                                new Thread(() -> {
                                    String latestVersion = VersionChecker.getLatestVersion();

                                    if (latestVersion == null) {
                                        source.sendMessage(Component.text("Unable to check for updates.").color(NamedTextColor.RED));
                                        return;
                                    }

                                    if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                                        source.sendMessage(Component.text("A new version is available: " + latestVersion).color(NamedTextColor.RED));
                                        source.sendMessage(Component.text("Please download the latest release: ")
                                                .append(Component.text("here")
                                                        .color(NamedTextColor.BLUE)
                                                        .decorate(TextDecoration.UNDERLINED)
                                                        .clickEvent(ClickEvent.openUrl(VersionChecker.getDownloadUrl()))));
                                    } else {
                                        source.sendMessage(Component.text("You are running the latest version: " + currentVersion).color(NamedTextColor.GREEN));
                                    }
                                }).start();

                                return 1;
                            }
                            context.getSource().sendMessage(Component.text("You do not have permission to check the version.", NamedTextColor.RED));
                            return 0;
                        })
                        .build())
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .executes(context -> {
                            CommandSource source = context.getSource();
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
                        })
                        .build())
                .build();

        BrigadierCommand brigadierCommand = new BrigadierCommand(commandBridgeNode);
        server.getCommandManager().register("commandbridge", brigadierCommand, "cb");
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            verboseLogger.warn("Player object is null");
            return;
        }

        verboseLogger.info("Checking for updates...");

        server.getScheduler().buildTask(plugin, () -> {
            String currentVersion = plugin.getVersion();
            String latestVersion = VersionChecker.getLatestVersion();

            if (latestVersion == null) {
                player.sendMessage(Component.text("Unable to check for updates.").color(NamedTextColor.RED));
                verboseLogger.warn("Unable to check for updates: latestVersion is null.");
                return;
            }

            if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                player.sendMessage(Component.text("A new version of CommandBridge is available: " + latestVersion).color(NamedTextColor.RED));
                player.sendMessage(Component.text("Please download the latest release: ")
                        .append(Component.text("here")
                                .color(NamedTextColor.BLUE)
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl(VersionChecker.getDownloadUrl()))));
                verboseLogger.info("Notified player " + player.getUsername() + " about the new version: " + latestVersion);
            } else {
                verboseLogger.info("Player " + player.getUsername() + " is running the latest version: " + currentVersion);
            }
        }).schedule();
    }
}
