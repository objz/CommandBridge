package dev.consti.bukkit.core;

import dev.consti.bukkit.Main;
import dev.consti.logging.Logger;
import dev.consti.utils.VersionChecker;

public class Startup {
    private final Logger logger;

    public Startup(Logger logger) {
        this.logger = logger;
    }

    public void start() {
        logger.info("Starting CommandBridge...");
        var runtime = Runtime.getInstance();

        try {
            runtime.getConfig().copyConfig("bukkit-config.yml", "config.yml");
            runtime.getConfig().loadAllConfigs();

            boolean debugMode = Boolean.parseBoolean(runtime.getConfig().getKey("config.yml", "debug"));
            logger.setDebug(debugMode);
            logger.debug("Debug mode set to: {}", debugMode);

            logger.debug("Copying default scripts...");
            runtime.getScriptUtils().copyDefaultScript("velocity-example.yml", "example.yml");
            runtime.getScriptUtils().loadAllScripts();

            logger.debug("Starting WebSocket server...");
            runtime.getClient().connect(
                    runtime.getConfig().getKey("config.yml", "remote"),
                    Integer.parseInt(runtime.getConfig().getKey("config.yml", "port"))
            );

            logger.debug("Setting up version checker...");
            VersionChecker.setProjectId("wiuI4ru2");

            logger.debug("Checking for updates...");
            checkForUpdates();

            logger.info("CommandBridge started successfully");
        } catch (Exception e) {
            logger.error("Failed to start CommandBridge. Error: {}", e.getMessage(), e);

        }
    }

    public void stop() {
        logger.info("Stopping CommandBridge...");
        var runtime = Runtime.getInstance();

        try {
            logger.debug("Stopping WebSocket server...");
            runtime.getClient().disconnect();
            logger.info("CommandBridge stopped successfully.");
        } catch (Exception e) {
            logger.error("Failed to stop CommandBridge. Error: {}", e.getMessage(), e);
        }
    }

    private void checkForUpdates() {
        String currentVersion = Main.getVersion();
        logger.debug("Current version: {}", currentVersion);

        new Thread(() -> {
            try {
                String latestVersion = VersionChecker.getLatestVersion();
                if (latestVersion == null) {
                    logger.info("Unable to check for updates.");
                    return;
                }

                logger.debug("Latest version retrieved: {}", latestVersion);
                if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                    logger.warn("A new version is available: {}", latestVersion);
                    logger.warn("Please download the latest release: {}", VersionChecker.getDownloadUrl());
                } else {
                    logger.info("You are running the latest version: {}", currentVersion);
                }
            } catch (Exception e) {
                logger.error("Error while checking for updates: {}", e.getMessage(), e);
            }
        }).start();
    }
}
