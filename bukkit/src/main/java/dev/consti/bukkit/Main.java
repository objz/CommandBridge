package dev.consti.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import dev.consti.bukkit.core.Runtime;
import dev.consti.logging.Logger;

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
    public void onEnable() {
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
        getLoggerInst().info("Stopping CommandBridge");
        try {
            runtime.getStartup().stop();
            getLoggerInst().info("CommandBridge stopped successfully.");
        } catch (Exception e) {
            getLoggerInst().error("Failed to stop CommandBridge: {}", e);
        }
    }

}
