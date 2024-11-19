package dev.consti.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import dev.consti.logging.Logger;
import dev.consti.utils.ScriptManager;
import dev.consti.velocity.Main;
import dev.consti.velocity.core.Runtime;
import dev.consti.velocity.utils.ProxyUtils;
import com.velocitypowered.api.proxy.ProxyServer;

public class CommandRegistrar {
    private final Logger logger;
    private final ProxyServer proxy;
    private final CommandHelper helper;

    public CommandRegistrar(Logger logger) {
        this.logger = logger;
        this.proxy = ProxyUtils.getProxyServer();
        this.helper = Runtime.getInstance().getHelper();
        logger.debug("CommandRegistrar initialized with helper: {}", helper.getClass().getSimpleName());
    }

    public void registerCommand(ScriptManager.ScriptConfig script) {
        String commandName = script.getName();
        logger.debug("Starting registration for command: {}", commandName);

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
                                logger.debug("Command {} called with arguments: {}", commandName, argsString);
                                String[] args = argsString.split(" ");
                                return helper.executeScriptCommands(context.getSource(), script, args);
                            })
            );

            LiteralCommandNode<CommandSource> rootNode = commandBuilder.build();
            BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
            CommandMeta commandMeta = proxy.getCommandManager().metaBuilder(commandName).build();

            proxy.getCommandManager().register(commandMeta, brigadierCommand);
            logger.info("Command {} registered successfully.", commandName);
        } catch (Exception e) {
            logger.error("Failed to register command {}. Error: {}", commandName, e.getMessage(), e);
        }
    }
}
