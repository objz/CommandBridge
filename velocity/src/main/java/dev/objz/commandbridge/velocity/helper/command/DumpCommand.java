package dev.objz.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.json.MessageBuilder;
import dev.objz.commandbridge.velocity.core.Runtime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;

public class DumpCommand {
    public static LiteralArgumentBuilder<CommandSource> build(Set<String> connectedClients, Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("dump")
                .executes(context -> {
                    CommandSource source = context.getSource();

                    if (!source.hasPermission("commandbridge.admin")) {
                        source.sendMessage(Component.text("You do not have permission to list connected clients",
                                NamedTextColor.RED));
                        return 0;
                    }

                    MessageBuilder builder = new MessageBuilder("system");
                    builder.addToBody("channel", "task").addToBody("task", "dump")
                            .addToBody("server", Runtime.getInstance().getConfig().getKey("config.yml", "server-id"));

                    Runtime.getInstance().getServer().broadcastServerMessage(builder.build());

                    String compact = Runtime.getInstance().getEncoder().encode();
                    String compressed;

                    try {
                        compressed = Runtime.getInstance().getEncoder().compress(compact);
                    } catch (Exception e) {
                        logger.error("Failed to compress the dump data: {}", logger.getDebug() ? e : e.getMessage());
                        source.sendMessage(
                                Component.text("Error while processing dump data").color(NamedTextColor.RED));
                        return 0;
                    }

                    source.sendMessage(Component.text(compact).color(NamedTextColor.LIGHT_PURPLE));
                    source.sendMessage(Component.text("===== Dumped Data =======").color(NamedTextColor.GOLD));
                    source.sendMessage(Component.text(compressed).color(NamedTextColor.GREEN));
                    source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));

                    return 1;
                });
    }
}
