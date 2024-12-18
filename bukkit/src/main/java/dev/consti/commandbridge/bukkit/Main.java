package dev.consti.commandbridge.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import dev.consti.commandbridge.bukkit.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

public class Main extends JavaPlugin {
    private static Main instance;
    private final Logger logger;

    public Main() {
        instance = this;
        logger = Runtime.getInstance().getLogger();
    }
    public static String getVersion() {
        return "2.0.0";
    }

    public static Main getInstance() {
        return instance;
    }


    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(false).usePluginNamespace().silentLogs(true));
    }
    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        logger.info("Initializing CommandBridge...");
        Runtime.getInstance().getStartup().start();
        logger.info("CommandBridge initialized successfully");
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        logger.info("Stopping CommandBridge...");
        Runtime.getInstance().getStartup().stop();
        logger.info("CommandBridge stopped successfully");
    }

}
