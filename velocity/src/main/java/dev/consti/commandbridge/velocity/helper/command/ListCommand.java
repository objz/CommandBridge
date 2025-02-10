package dev.consti.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.consti.foundationlib.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;

public class ListCommand {
    public static LiteralArgumentBuilder<CommandSource> build(Set<String> connectedClients, Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("list")
                .executes(context -> {
                    CommandSource source = context.getSource();

                    if (!source.hasPermission("commandbridge.admin")) {
                        source.sendMessage(Component.text("You do not have permission to list connected clients", NamedTextColor.RED));
                        return 0;
                    }

                    if (connectedClients.isEmpty()) {
                        source.sendMessage(
                                Component.text("No clients are currently connected").color(NamedTextColor.RED));
                    } else {
                        String clientsString = String.join(", ", connectedClients);
                        source.sendMessage(Component.text("===== Connected Clients =======").color(NamedTextColor.GOLD));
                        source.sendMessage(Component.text(clientsString).color(NamedTextColor.GREEN));
                        source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));
                    }
                    return 1;
                });
    }
}


