package dev.consti.commandbridge.bukkit.core;

import dev.consti.commandbridge.bukkit.Main;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.VersionChecker;

public class Startup {
    private final Logger logger;
    private final Runtime runtime;

    public Startup(Logger logger) {
        this.logger = logger;
        this.runtime = Runtime.getInstance();
    }

    public void start() {
        try {
            runtime.getConfig().copyConfig("bukkit-config.yml", "config.yml");
            runtime.getConfig().loadAllConfigs();

            boolean debugMode = Boolean.parseBoolean(runtime.getConfig().getKey("config.yml", "debug"));
            logger.setDebug(debugMode);
            logger.info("Debug mode set to: {}", debugMode);

            logger.debug("Copying default scripts...");
            runtime.getScriptUtils().copyDefaultScript("velocity-example.yml", "example.yml");
            runtime.getScriptUtils().loadAllScripts();

            logger.debug("Connecting to WebSocket server...");
            runtime.getClient().connect(
                    runtime.getConfig().getKey("config.yml", "remote"),
                    Integer.parseInt(runtime.getConfig().getKey("config.yml", "port"))
            );

            logger.debug("Setting up version checker...");
            VersionChecker.setProjectId("wIuI4ru2");

            logger.debug("Checking for updates...");
            checkForUpdates();
        } catch (Exception e) {
            logger.error("Failed to initialize CommandBridge: {}",
                    logger.getDebug() ? e : e.getMessage()
                    );
        }
    }

    public void stop() {
        try {
            logger.debug("Disconnecting from WebSocket server...");
            runtime.getClient().disconnect();
        } catch (Exception e) {
            logger.error("Failed to stop CommandBridge: {}",
                    logger.getDebug() ? e : e.getMessage()
                    );
        }
    }

    private void checkForUpdates() {
        String currentVersion = Main.getVersion();
        logger.debug("Current version: {}", currentVersion);
        new Thread(() -> {
            try {
                String latestVersion = VersionChecker.getLatestVersion();
                if (latestVersion == null) {
                    logger.warn("Unable to check for updates");
                    return;
                }
                if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                    logger.warn("A new version is available: {}", latestVersion);
                    logger.warn("Please download the latest release: {}", VersionChecker.getDownloadUrl());
                } else {
                    logger.info("You are running the latest version: {}", currentVersion);
                }
            } catch (Exception e) {
                logger.error("Error while checking for updates: {}",
                        logger.getDebug() ? e : e.getMessage()
                        );
            }
        }).start();
    }
}
