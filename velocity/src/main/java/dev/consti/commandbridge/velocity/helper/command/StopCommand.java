package dev.consti.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


public class StopCommand {
    public static LiteralArgumentBuilder<CommandSource> build(Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("stop")
                .executes(context -> {
                    CommandSource source = context.getSource();

                    Runtime.getInstance().getStartup().stop();
                    source.sendMessage(Component.text("Websocket Server stopped").color(NamedTextColor.YELLOW));
                    return 1;
                });
    }
}

