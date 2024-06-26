package org.commandbridge.command.utils;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commandbridge.utilities.StringParser.parsePlaceholders;

public class CommandRegistrar {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final CommandExecutor commandExecutor;

    public CommandRegistrar(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.commandExecutor = new CommandExecutor(server, plugin);
        this.verboseLogger = plugin.getVerboseLogger();
    }



    public void registerCommand(Map<String, Object> commandData) {
        commandExecutor.resetState();
        String commandName = (String) commandData.get("name");
        List<Map<String, Object>> commandList = (List<Map<String, Object>>) commandData.get("commands");
        boolean disableExecutorIsPlayerCheck = (boolean) commandData.getOrDefault("disable-check-if-executor-is-player", false);
        if (disableExecutorIsPlayerCheck) {
            verboseLogger.warn("Executor is player check is disabled for command " + commandName);
        } else {
            verboseLogger.info("Executor is player check is enabled for command " + commandName);
        }

        if (commandName == null || commandList == null || commandList.isEmpty()) {
            verboseLogger.error("Command name or command list is missing or empty in config.", new IllegalArgumentException());
            return;
        }

        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> disableExecutorIsPlayerCheck || source instanceof Player)
                .executes(context -> {
                    CommandSource source = context.getSource();

                    if (!disableExecutorIsPlayerCheck && !(source instanceof Player player)) {
                        verboseLogger.warn("This command can only be used by a player.");
                        return 0;
                    }
                    Player player = (Player) source; // Casting sicher durchführen

                    if (!source.hasPermission("commandbridge.command." + commandName)) {
                        source.sendMessage(Component.text("You do not have permission to use this command.", net.kyori.adventure.text.format.NamedTextColor.RED));
                        return 0;
                    }
                    commandExecutor.resetState();

                    for (Map<String, Object> cmdData : commandList) {
                        String cmd = parsePlaceholders((String) cmdData.get("command"), player);
                        int delay = (int) cmdData.getOrDefault("delay", 0);
                        String targetServerId = (String) cmdData.get("target-server-id");
                        String targetExecutor = (String) cmdData.getOrDefault("target-executor", "console");
                        boolean waitForOnline = (boolean) cmdData.getOrDefault("wait-until-player-is-online", false);
                        boolean disablePlayerOnline = (boolean) cmdData.getOrDefault("disable-check-if-executor-is-on-server", false);

                        if (disablePlayerOnline) {
                            verboseLogger.warn("Player online check is disabled for command " + commandName);
                        } else {
                            verboseLogger.info("Player online check is enabled for command " + commandName);
                        }

                        if (delay > 0) {
                            server.getScheduler().buildTask(plugin, () -> commandExecutor.executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline))
                                    .delay(delay, TimeUnit.SECONDS)
                                    .schedule();
                        } else {
                            commandExecutor.executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline);
                        }
                    }
                    return 1;
                })
                .build();

        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        server.getCommandManager().register(brigadierCommand);
        plugin.addRegisteredCommand(commandName);
        verboseLogger.info("Command " + commandName + " registered successfully.");
    }

    public void registerBukkitCommand(Map<String, Object> commandData) {
        String command = (String) commandData.get("name");
        List<String> targetServerIds = (List<String>) commandData.get("target-server-ids");

        if (command == null || targetServerIds == null || targetServerIds.isEmpty()) {
            verboseLogger.warn("Command or target server IDs are missing in config.");
            return;
        }

        for (String targetServerId : targetServerIds) {
            plugin.getBridge().registerBukkitCommand(command, targetServerId);
        }
    }

    public void unregisterBukkitCommand(Map<String, Object> data) {
        String command = (String) data.get("name");
        List<String> targetServerIds = (List<String>) data.get("target-server-ids");

        if (command == null || targetServerIds == null || targetServerIds.isEmpty()) {
            verboseLogger.warn("Command or target server IDs are missing in config.");
            return;
        }

        for (String targetServerId : targetServerIds) {
            plugin.getBridge().unregisterBukkitCommand(command, targetServerId);
        }

    }
}
