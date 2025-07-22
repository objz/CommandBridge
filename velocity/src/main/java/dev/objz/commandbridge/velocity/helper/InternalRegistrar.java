package dev.objz.commandbridge.velocity.helper;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.velocity.Main;
import dev.objz.commandbridge.velocity.core.Runtime;
import dev.objz.commandbridge.velocity.helper.command.DumpCommand;
import dev.objz.commandbridge.velocity.helper.command.ListCommand;
import dev.objz.commandbridge.velocity.helper.command.ReloadCommand;
import dev.objz.commandbridge.velocity.helper.command.StartCommand;
import dev.objz.commandbridge.velocity.helper.command.StopCommand;
import dev.objz.commandbridge.velocity.helper.command.VersionCommand;
import dev.objz.commandbridge.core.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;

public class InternalRegistrar {
    private final Logger logger;
    private final ProxyServer proxy;
    private final Main plugin;
    private final Set<String> connectedClients;

    public InternalRegistrar(Logger logger, ProxyServer proxy, Main plugin, Set<String> connectedClients) {
        this.logger = logger;
        this.proxy = proxy;
        this.plugin = plugin;
        this.connectedClients = connectedClients;
    }

    public void registerCommands() {
        logger.info("Registering commands for CommandBridge...");
        try {
            LiteralArgumentBuilder<CommandSource> commandBridgeBuilder = LiteralArgumentBuilder
                    .<CommandSource>literal("commandbridge")
                    .executes(context -> {
                        if (context.getSource().hasPermission("commandbridge.admin")) {
                            return HelpCommand.sendHelpMessage(context.getSource(), logger);
                        }
                        context.getSource().sendMessage(
                                Component.text("You do not have permission to use this command", NamedTextColor.RED));
                        return 0;
                    });

            commandBridgeBuilder.then(ReloadCommand.build(Runtime.getInstance().getGeneralUtils(), logger));
            commandBridgeBuilder.then(VersionCommand.build(logger));
            commandBridgeBuilder.then(HelpCommand.build(logger));
            commandBridgeBuilder.then(ListCommand.build(connectedClients, logger));
            // commandBridgeBuilder.then(DumpCommand.build(connectedClients, logger));
            if (logger.getDebug()) {
                commandBridgeBuilder.then(StopCommand.build(logger));
                commandBridgeBuilder.then(StartCommand.build(logger));
            }

            LiteralCommandNode<CommandSource> commandBridgeNode = commandBridgeBuilder.build();

            Runtime.getInstance().getGeneralUtils().setMeta(proxy.getCommandManager()
                    .metaBuilder("commandbridge")
                    .aliases("cb")
                    .plugin(plugin)
                    .build());

            BrigadierCommand brigadierCommand = new BrigadierCommand(commandBridgeNode);
            proxy.getCommandManager().register(Runtime.getInstance().getGeneralUtils().getMeta(), brigadierCommand);
            logger.info("CommandBridge commands registered successfully");
        } catch (Exception e) {
            logger.error("Failed to register CommandBridge commands: {}", logger.getDebug() ? e : e.getMessage());
        }
    }

}
