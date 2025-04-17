package dev.consti.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.util.GeneralUtils;
import dev.consti.foundationlib.json.MessageBuilder;
import dev.consti.foundationlib.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadCommand {

    public static LiteralArgumentBuilder<CommandSource> build(GeneralUtils utils, Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("reload")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (!source.hasPermission("commandbridge.admin")) {
                        source.sendMessage(
                                Component.text("You do not have permission to reload the plugin", NamedTextColor.RED));
                        return 0;
                    }

                    try {
                        // Logic from original reload command
                        Runtime.getInstance().getRegistrar().unregisterAllCommands();
                        logger.debug("All commands have been unregistered");

                        Runtime.getInstance().getConfig().reload();
                        logger.debug("Configuration files have been reloaded");

                        logger.setDebug(
                                Boolean.parseBoolean(Runtime.getInstance().getConfig().getKey("config.yml", "debug")));
                        logger.info("Debug mode set to: {}", Runtime.getInstance().getConfig().getKey("config.yml", "debug"));

                        Runtime.getInstance().getScriptUtils().reload();
                        logger.debug("Scripts have been reloaded");

                        Runtime.getInstance().getGeneralUtils().unregisterCommands();
                        logger.debug("Internal commands have been unregistered");

                        Runtime.getInstance().getGeneralUtils().registerCommands();
                        logger.debug("Internal commands have been registered");

                        MessageBuilder builder = new MessageBuilder("system");
                        builder.addToBody("channel", "task").addToBody("task", "reload").addToBody("server",
                                Runtime.getInstance().getConfig().getKey("config.yml", "server-id"));

                        Runtime.getInstance().getServer().broadcastServerMessage(builder.build());
                        logger.debug("Sending payload: {}", builder.build());
                        source.sendMessage(
                                Component.text("Waiting for clients to respond...")
                                        .color(NamedTextColor.YELLOW));
                        logger.debug("Waiting for clients to respond...");
                        utils.startFailureCheck(source);
                        return 1;
                    } catch (Exception e) {
                        logger.error("An error occurred during the reload process: {}", e.getMessage(), e);
                        source.sendMessage(
                                Component.text("ReloadCommand failed due to an internal error. Check logs for details")
                                        .color(NamedTextColor.RED));
                    }
                    return 1;

                });
    }
}
