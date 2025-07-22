package dev.objz.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.json.MessageBuilder;
import dev.objz.commandbridge.velocity.core.Runtime;
import dev.objz.commandbridge.velocity.helper.DumpFailureChecker;
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

                    source.sendMessage(
                            Component.text("Generating Dump...")
                                    .color(NamedTextColor.YELLOW));

                    new DumpFailureChecker(source, logger).run(); 

                    return 1;
                });
    }
}
