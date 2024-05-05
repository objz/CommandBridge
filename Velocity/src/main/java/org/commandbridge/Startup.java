package org.commandbridge;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

public class Startup {

    private final ProxyServer server;
    private final CommandBridge plugin;

    public Startup(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
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
