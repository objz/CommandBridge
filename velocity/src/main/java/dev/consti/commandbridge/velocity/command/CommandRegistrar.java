package dev.consti.commandbridge.velocity.command;

import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.utils.ProxyUtils;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;

public class CommandRegistrar {
    private final Logger logger;
    private final ProxyServer proxy;
    private final CommandHelper helper;
    private final Map<String, CommandMeta> registeredCommands = new HashMap<>();

    public CommandRegistrar(Logger logger) {
        this.logger = logger;
        this.proxy = ProxyUtils.getProxyServer();
        this.helper = Runtime.getInstance().getHelper();
    }


    public void unregisterAllCommands() {
        for (String command : registeredCommands.keySet()) {
            try {
                CommandMeta commandMeta = registeredCommands.get(command);
                proxy.getCommandManager().unregister(commandMeta);
                logger.debug("Unregistered command: {}", command);
            } catch (Exception e) {
                logger.error("Failed to unregister command '{}' : {}",
                        command,
                        logger.getDebug() ? e : e.getMessage()
                );
            }
        }
        registeredCommands.clear();
        logger.info("All registered commands have been unregistered.");
    }


    public void registerCommand(ScriptManager.ScriptConfig script) {
        String commandName = script.getName();
        try {
            LiteralArgumentBuilder<CommandSource> commandBuilder = LiteralArgumentBuilder.<CommandSource>literal(commandName)
                    .executes(context -> {
                        logger.debug("Executing base command: {}", commandName);
                        return helper.executeScriptCommands(context.getSource(), script, new String[0]);
                    });
            commandBuilder.then(
                    RequiredArgumentBuilder.<CommandSource, String>argument("args", StringArgumentType.greedyString())
                            .executes(context -> {
                                String argsString = context.getArgument("args", String.class);
                                logger.debug("Command '{}' called with arguments: {}", commandName, argsString);
                                String[] args = argsString.split(" ");
                                return helper.executeScriptCommands(context.getSource(), script, args);
                            })
            );

            LiteralCommandNode<CommandSource> rootNode = commandBuilder.build();
            BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
            CommandMeta commandMeta = proxy.getCommandManager().metaBuilder(commandName).build();

            proxy.getCommandManager().register(commandMeta, brigadierCommand);
            registeredCommands.put(commandName, commandMeta);
            logger.debug("Added command '{}' to registeredCommands: {}",commandName, commandMeta);
        } catch (Exception e) {
            logger.error(
                    "Failed to register command '{}' : {}",
                    commandName,
                    logger.getDebug() ? e : e.getMessage()
            );
        }
    }


}
