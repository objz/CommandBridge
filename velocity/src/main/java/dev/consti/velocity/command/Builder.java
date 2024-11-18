package dev.consti.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.logging.Logger;
import dev.consti.utils.ScriptManager;
import dev.consti.utils.StringParser;
import dev.consti.velocity.Main;
import dev.consti.velocity.Runtime;
import dev.consti.velocity.utils.ProxyServerHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.concurrent.TimeUnit;

public class Builder {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Main plugin;

    public Builder() {
        this.proxy = ProxyServerHolder.getProxyServer();
        this.logger = Runtime.getInstance().getLogger();
        this.plugin = Main.getInstance();
    }

    public void registerCommand(ScriptManager.ScriptConfig script) {
        String commandName = script.getName();

        LiteralArgumentBuilder<CommandSource> commandBuilder = LiteralArgumentBuilder.<CommandSource>literal(commandName)
                .executes(context -> executeScriptCommands(context.getSource(), script, new String[0]));

        commandBuilder.then(
                RequiredArgumentBuilder.<CommandSource, String>argument("args", StringArgumentType.greedyString())
                        .executes(context -> {
                            String argsString = context.getArgument("args", String.class);
                            String[] args = argsString.split(" ");
                            return executeScriptCommands(context.getSource(), script, args);
                        })
        );

        LiteralCommandNode<CommandSource> rootNode = commandBuilder.build();
        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        CommandMeta commandMeta = proxy.getCommandManager().metaBuilder(commandName).build();
        proxy.getCommandManager().register(commandMeta, brigadierCommand);

        logger.info("Command {} registered successfully.", commandName);
    }

    private int executeScriptCommands(CommandSource source, ScriptManager.ScriptConfig script, String[] args) {
        if (!script.shouldIgnorePermissionCheck() && !source.hasPermission("commandbridge.command." + script.getName())) {
            if (!script.shouldHidePermissionWarning()) {
                source.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            }
            return 0;
        }

        for (ScriptManager.Command cmd : script.getCommands()) {
            if (cmd.isCheckIfExecutorIsPlayer() && !(source instanceof Player)) {
                logger.warn("This command can only be used by a player.");
                return 0;
            }

            processCommand(cmd, source, args);
        }
        return Command.SINGLE_SUCCESS;
    }

    private void processCommand(ScriptManager.Command cmd, CommandSource source, String[] args) {
        Player player = (source instanceof Player) ? (Player) source : null;
        StringParser parser = StringParser.create();

        if (player != null) {
            parser.addPlaceholder("%player%", player.getUsername());
            parser.addPlaceholder("%uuid%", player.getUniqueId().toString());
            parser.addPlaceholder("%server%", player.getCurrentServer()
                    .map(serverConnection -> serverConnection.getServerInfo().getName())
                    .orElse("defaultServerName"));

            // Check if the player is on the required server
            if (cmd.isCheckIfExecutorIsOnServer() && player.getCurrentServer().isPresent()) {
                String currentServer = player.getCurrentServer().get().getServerInfo().getName();
                if (!cmd.getTargetServerIds().contains(currentServer)) {
                    logger.warn("Player {} is not on the required server for this command.", player.getUsername());
                    return;
                }
            }
        } else {
            logger.warn("Player is null");
            return;
        }

        String commandStr = parser.parsePlaceholders(cmd.getCommand(), args);

        if (cmd.getDelay() > 0) {
            proxy.getScheduler().buildTask(plugin, () -> executeCommand(cmd, commandStr, player))
                    .delay(cmd.getDelay(), TimeUnit.SECONDS)
                    .schedule();
        } else {
            executeCommand(cmd, commandStr, player);
        }
    }

    private void executeCommand(ScriptManager.Command cmd, String commandStr, Player player, int retryCount) {
        // Check if maximum retries are reached
        if (retryCount >= 30) {
            logger.warn("Max retries reached for command: {}", cmd.getCommand());
            return;
        }

        // Check if the player is online
        if (cmd.shouldWaitUntilPlayerIsOnline() && (player == null || !player.isActive())) {
            proxy.getScheduler().buildTask(plugin, () -> executeCommand(cmd, commandStr, player, retryCount + 1))
                    .delay(1, TimeUnit.SECONDS)
                    .schedule();
            return;
        }

        // Handle server-specific execution
        if (!cmd.getTargetServerIds().isEmpty()) {
            for (String serverId : cmd.getTargetServerIds()) {
                proxy.getServer(serverId).ifPresentOrElse(
                        server -> {
                            Runtime.getInstance().getServer().sendJSON(commandStr, serverId, new String[0], player);
                        },
                        () -> logger.warn("Server {} not found", serverId)
                );
            }
        } else {
            logger.warn("Target server ids are empty, check your script");
        }
    }

    // Overloaded function for initial call
    private void executeCommand(ScriptManager.Command cmd, String commandStr, Player player) {
        executeCommand(cmd, commandStr, player, 0); // Default retry count = 0
    }


}

