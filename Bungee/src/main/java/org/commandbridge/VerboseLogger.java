package org.commandbridge;

import net.md_5.bungee.api.plugin.Plugin;
import java.util.logging.Logger;

public class VerboseLogger {
    private final CommandBridgeBungee plugin;
    private final Logger logger;

    public VerboseLogger(CommandBridgeBungee plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void info(String message) {
        if (plugin.isVerboseOutputEnabled()) {
            logger.info(message);
        }
    }

    public void warn(String message) {
        if (plugin.isVerboseOutputEnabled()) {
            logger.warning(message); // Note that in java.util.logging, it's 'warning' not 'warn'
        }
    }

    public void error(String message, Throwable e) {
        if (plugin.isVerboseOutputEnabled()) {
            logger.log(java.util.logging.Level.SEVERE, message, e); // Use 'log' for error messages with exceptions
        }
    }
}
