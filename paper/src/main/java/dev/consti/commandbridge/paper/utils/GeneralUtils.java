package dev.consti.commandbridge.paper.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

import dev.consti.commandbridge.paper.Main;
import dev.consti.foundationlib.logging.Logger;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.consti.commandbridge.paper.core.Runtime;

public class GeneralUtils {
    private final Logger logger;

    public GeneralUtils(Logger logger) {
        this.logger = logger;
    }

    public void reloadAll() {
        new SchedulerAdapter(Main.getInstance())
                .run(
                        () -> {
                            logger.debug("Running on thread (reload): {}", Thread.currentThread().getName());
                            try {
                                Runtime.getInstance().getConfig().reload();
                                logger.debug("All configs have been reloaded");
                                logger.setDebug(Boolean
                                        .parseBoolean(Runtime.getInstance().getConfig().getKey("config.yml", "debug")));
                                logger.info("Debug mode set to: {}",
                                        Runtime.getInstance().getConfig().getKey("config.yml", "debug"));
                                Runtime.getInstance().getScriptUtils().reload();
                                logger.debug("All scripts have been reloaded");
                                logger.info("Everything Reloaded!");
                                Runtime.getInstance().getClient().sendTask("reload", "success");
                            } catch (Exception e) {
                                logger.error("Error occurred while reloading: {}",
                                        logger.getDebug() ? e : e.getMessage());
                                Runtime.getInstance().getClient().sendTask("reload", "failure");
                            }
                        });
    }

    public void registerCommands() {
        List<Argument<?>> arguments = new ArrayList<>();
        arguments.add(new StringArgument("arguments").replaceSuggestions(ArgumentSuggestions.strings("reconnect")));
        new CommandAPICommand("commandbridgeclient")
                .withArguments(arguments)
                .withAliases("cbc")
                .withPermission("commandbridge.admin")
                .executes((sender, args) -> {
                    String opt = (String) args.get("arguments");
                    if (opt.matches("reconnect")) {
                        Runtime.getInstance().getClient().disconnect();
                        try {
                            Runtime.getInstance().getClient().connect(
                                    Runtime.getInstance().getConfig().getKey("config.yml", "remote"),
                                    Integer.parseInt(Runtime.getInstance().getConfig().getKey("config.yml", "port")));
                        } catch (Exception e) {
                            logger.error("Client reconnection failed: ", e);
                            sender.sendMessage(ChatColor.RED + "Failed to reconnect");
                        }
                        sender.sendMessage(ChatColor.GREEN + "Client reconnected successfully");
                    }
                })
                .register();
    }

}
