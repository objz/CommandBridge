package org.commandbridge.command.utils;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        List<Map<String, Object>> commandList = safeCastToListOfMaps(commandData.get("commands"));
        boolean disableExecutorIsPlayerCheck = (boolean) commandData.getOrDefault("disable-check-if-executor-is-player", false);
        boolean registerOnBukkitServer = (boolean) commandData.getOrDefault("register-on-bukkit-server", false);

        logExecutorCheckState(commandName, disableExecutorIsPlayerCheck);

        if (!isValidCommandData(commandName, commandList)) {
            verboseLogger.error("Command name or command list is missing or empty in config.", new IllegalArgumentException());
            return;
        }

        LiteralCommandNode<CommandSource> rootNode = createRootNode(commandName, commandList, disableExecutorIsPlayerCheck);
        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);

        server.getCommandManager().register(brigadierCommand);
        plugin.addRegisteredCommand(commandName);
        verboseLogger.info("Command " + commandName + " registered successfully.");

        if (registerOnBukkitServer) {
            registerCommandOnBukkit(commandName, commandList);
        }
    }

    private void logExecutorCheckState(String commandName, boolean disableExecutorIsPlayerCheck) {
        if (disableExecutorIsPlayerCheck) {
            verboseLogger.warn("Executor is player check is disabled for command " + commandName);
        } else {
            verboseLogger.info("Executor is player check is enabled for command " + commandName);
        }
    }

    private boolean isValidCommandData(String commandName, List<Map<String, Object>> commandList) {
        return commandName != null && commandList != null && !commandList.isEmpty();
    }

    private LiteralCommandNode<CommandSource> createRootNode(String commandName, List<Map<String, Object>> commandList, boolean disableExecutorIsPlayerCheck) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> disableExecutorIsPlayerCheck || source instanceof Player)
                .executes(context -> {
                    CommandSource source = context.getSource();

                    if (!disableExecutorIsPlayerCheck && !(source instanceof Player)) {
                        verboseLogger.warn("This command can only be used by a player.");
                        return 0;
                    }

                    Player player = (Player) source;

                    if (!source.hasPermission("commandbridge.command." + commandName)) {
                        source.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                        return 0;
                    }

                    commandExecutor.resetState();

                    for (Map<String, Object> cmdData : commandList) {
                        processCommandData(cmdData, player, commandName);
                    }
                    return 1;
                })
                .build();
    }

    private void processCommandData(Map<String, Object> cmdData, Player player, String commandName) {
        String cmd = parsePlaceholders((String) cmdData.get("command"), player);
        int delay = (int) cmdData.getOrDefault("delay", 0);
        String targetServerId = (String) cmdData.get("target-server-id");
        String targetExecutor = (String) cmdData.getOrDefault("target-executor", "console");
        boolean waitForOnline = (boolean) cmdData.getOrDefault("wait-until-player-is-online", false);
        boolean disablePlayerOnline = (boolean) cmdData.getOrDefault("disable-check-if-executor-is-on-server", false);

        logPlayerOnlineCheckState(commandName, disablePlayerOnline);

        if (delay > 0 ) {
            scheduleCommandExecution(cmd, targetServerId, targetExecutor, waitForOnline, player, disablePlayerOnline, delay);
        } else {
            commandExecutor.executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline);
        }
    }

    private void logPlayerOnlineCheckState(String commandName, boolean disablePlayerOnline) {
        if (disablePlayerOnline) {
            verboseLogger.warn("Player online check is disabled for command " + commandName);
        } else {
            verboseLogger.info("Player online check is enabled for command " + commandName);
        }
    }

    private void scheduleCommandExecution(String cmd, String targetServerId, String targetExecutor, boolean waitForOnline, Player player, boolean disablePlayerOnline, int delay) {
        server.getScheduler().buildTask(plugin, () -> commandExecutor.executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline))
                .delay(delay, TimeUnit.SECONDS)
                .schedule();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeCastToListOfMaps(Object obj) {
        if (obj instanceof List<?> list) {
            if (!list.isEmpty() && list.getFirst() instanceof Map) {
                return (List<Map<String, Object>>) list;
            }
        }
        return null;
    }

    private void registerCommandOnBukkit(String commandName, List<Map<String, Object>> commandList) {

    }
}
