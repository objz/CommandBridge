package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;

public class VerboseLogger {
    private boolean verboseOutput;
    private final CommandBridge plugin;

    public VerboseLogger(CommandBridge plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
    this.verboseOutput = plugin.getConfig().getBoolean("verbose-output", false);
}
    
    public void info(String message) {
        if (verboseOutput) {
            plugin.getLogger().info(message);
        }
    }
    
    public void warn(String message) {
        if (verboseOutput) {
            plugin.getLogger().warning(message);
        }
    }
    
    public void error(String message, Throwable e) {
        if (verboseOutput) {
            plugin.getLogger().severe(message + " : " + e.getMessage());
        }
    }
}
