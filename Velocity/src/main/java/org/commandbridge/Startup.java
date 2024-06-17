package org.commandbridge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

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
            dataFolder.mkdirs();
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
            Yaml yaml = new Yaml(new Constructor(Map.class));
            Map<String, Object> data = yaml.load(fis);
            verboseOutput = (boolean) data.getOrDefault("verbose-output", false);
            verboseLogger.ForceInfo("Config loaded. Verbose output is " + (verboseOutput ? "enabled" : "disabled"));
            velocityRuntime.loadScripts();
            copyExampleYml();
        } catch (IOException e) {
            verboseLogger.error("Failed to load config file", e);
        }
    }

    public boolean isVerboseOutput() {
        return verboseOutput;
    }

    private void copyExampleYml() {
        Path scriptFolder = dataDirectory.resolve("scripts");
        try {
            Files.createDirectories(scriptFolder); // Stellt sicher, dass der Ordner existiert
            Path exampleScript = scriptFolder.resolve("example.yml");
            Path exampleBukkitScript = scriptFolder.resolve("example-bukkit.yml");
            if (Files.notExists(exampleScript) || Files.notExists(exampleBukkitScript)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("example.yml")) {
                    if (in == null) {
                        verboseLogger.warn("Could not find example.yml in resources");
                        return;
                    }
                    Files.copy(in, exampleScript, StandardCopyOption.REPLACE_EXISTING);
                    verboseLogger.info("example.yml has been copied successfully.");
                }
                try (InputStream in =
                     getClass().getClassLoader().getResourceAsStream("example-bukkit.yml")) {
                         if (in == null) {
                     verboseLogger.warn("Could not find example-bukkit.yml in resources");
                         return;
                    }
                              Files.copy(in, exampleBukkitScript, StandardCopyOption.REPLACE_EXISTING);
                         verboseLogger.info("example-bukkit.yml has been copied successfully.");
                    }
            }
        } catch (IOException e) {
            verboseLogger.error("Failed to create scripts folder or copy example.yml", e);
        }
    }

    public void registerCommands() {
        LiteralCommandNode<CommandSource> commandBridgeNode = LiteralArgumentBuilder.<CommandSource>literal("commandbridge")
                .executes(context -> {
                    if (context.getSource().hasPermission("commandbridge.admin")) {
                        context.getSource().sendMessage(Component.text("Use /commandbridge reload to reload scripts."));
                        return 1;
                    }
                    context.getSource().sendMessage(Component.text("You do not have permission to use this command.", net.kyori.adventure.text.format.NamedTextColor.RED));
                    return 0;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .executes(context -> {
                            if (context.getSource().hasPermission("commandbridge.admin")) {
                                plugin.getRuntime().loadScripts();
                                context.getSource().sendMessage(Component.text("Scripts reloaded!", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                                return 1;
                            }
                            context.getSource().sendMessage(Component.text("You do not have permission to reload scripts.", net.kyori.adventure.text.format.NamedTextColor.RED));
                            return 0;
                        })
                        .build())
                .build();

        BrigadierCommand brigadierCommand = new BrigadierCommand(commandBridgeNode);
        server.getCommandManager().register("commandbridge", brigadierCommand, "cb");
    }
}
