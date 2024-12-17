package dev.consti.commandbridge.velocity.core;

import dev.consti.commandbridge.velocity.Main;
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
            runtime.getConfig().copyConfig("velocity-config.yml", "config.yml");
            runtime.getConfig().loadAllConfigs();
            runtime.getConfig().loadSecret();

            boolean debugMode = Boolean.parseBoolean(runtime.getConfig().getKey("config.yml", "debug"));
            logger.setDebug(debugMode);
            logger.debug("Debug mode set to: {}", debugMode);

            logger.debug("Copying default scripts...");
            runtime.getScriptUtils().copyDefaultScript("velocity-example.yml", "example.yml");
            runtime.getScriptUtils().loadAllScripts();

            logger.debug("Starting WebSocket server...");
            runtime.getServer().startServer(
                    Integer.parseInt(runtime.getConfig().getKey("config.yml", "port")),
                    runtime.getConfig().getKey("config.yml", "host")
            );

            logger.debug("Setting up version checker...");
            VersionChecker.setProjectId("wIuI4ru2");

            logger.debug("Checking for updates...");
            checkForUpdates();

            logger.debug("Registering internal commands...");
            runtime.getGeneralUtils().registerCommands();
        } catch (Exception e) {
            logger.error("Failed to initialize CommandBridge: {}",
                    logger.getDebug() ? e : e.getMessage()
            );
        }
    }

    public void stop() {
        try {
            logger.debug("Stopping WebSocket server...");
            runtime.getServer().stopServer(
                    Integer.parseInt(runtime.getConfig().getKey("config.yml", "timeout"))
            );
        } catch (Exception e) {
            logger.error("Failed to stop CommandBridge: {}", logger.getDebug() ? e : e.getMessage());
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
                logger.error("Error while checking for updates: {}", logger.getDebug() ? e : e.getMessage());
            }
        }).start();
    }
}
