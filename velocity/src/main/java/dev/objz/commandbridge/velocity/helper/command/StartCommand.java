package dev.objz.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.core.Runtime;
import dev.objz.commandbridge.core.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class StartCommand {
    public static LiteralArgumentBuilder<CommandSource> build(Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("start")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (!source.hasPermission("commandbridge.admin")) {
                        source.sendMessage(Component.text("You do not have permission to start the WebSocket server",
                                NamedTextColor.RED));
                        return 0;
                    }
                    Runtime.getInstance().getServer().startServer(
                            Integer.parseInt(Runtime.getInstance().getConfig().getKey("config.yml", "port")),
                            Runtime.getInstance().getConfig().getKey("config.yml", "host"),
                            Runtime.getInstance().getConfig().getKey("config.yml", "san"));

                    source.sendMessage(Component.text("WebSocket Server started").color(NamedTextColor.GREEN));
                    return 1;
                });
    }
}
