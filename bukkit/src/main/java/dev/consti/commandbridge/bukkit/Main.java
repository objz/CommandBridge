package dev.consti.commandbridge.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import dev.consti.commandbridge.bukkit.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

public class Main extends JavaPlugin {
    private static Main instance;
    private Runtime runtime;

    public Main() {
        instance = this;
    }
    public static String getVersion() {
        return "2.0.0";
    }

    public static Main getInstance() {
        return instance;
    }

    public Logger getLoggerInst() {
        return Runtime.getInstance().getLogger();
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(false).usePluginNamespace().silentLogs(true));
    }
    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        getLoggerInst().info("Initializing CommandBridge...");
        runtime = Runtime.getInstance();
        try {
            runtime.getStartup().start();
            getLoggerInst().info("CommandBridge initialized successfully.");
        } catch (Exception e) {
            getLoggerInst().error("Failed to initialize CommandBridge: {}", e);
        }
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        getLoggerInst().info("Stopping CommandBridge");
        try {
            runtime.getStartup().stop();
            getLoggerInst().info("CommandBridge stopped successfully.");
        } catch (Exception e) {
            getLoggerInst().error("Failed to stop CommandBridge: {}", e);
        }
    }

}
