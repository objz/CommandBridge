package dev.consti.commandbridge.paper.utils;

import org.bukkit.Bukkit;

import dev.consti.commandbridge.paper.Main;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.commandbridge.paper.core.Runtime;

public class GeneralUtils {
    private final Logger logger;

    public GeneralUtils(Logger logger) {
        this.logger = logger;
    }

    public void reloadAll() {
        Bukkit.getScheduler()
                .runTask(
                        Main.getInstance(),
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

}
