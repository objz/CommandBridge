package dev.consti.bukkit;

import dev.consti.bukkit.utils.onScript;
import dev.consti.bukkit.websocket.Client;
import dev.consti.logging.Logger;
import dev.consti.utils.ConfigManager;
import dev.consti.utils.VersionChecker;

public class Runtime {
    private static Runtime instance;
    private final Logger logger;
    private final ConfigManager config;
    private final onScript onScript;
    private final Client client;

    private Runtime() {
        this.logger = new Logger("CommandBridge");
        this.config = new ConfigManager(logger, "CommandBridge");
        this.onScript = new onScript(logger, "CommandBridge");
        config.copyConfig("bukkit-config.yml", "config.yml");
        config.loadAllConfigs();
        logger.setDebug(Boolean.parseBoolean(config.getKey("config.yml", "debug")));
        this.client = new Client(logger, config.getKey("config.yml", "secret"));
    }

    public static synchronized Runtime getInstance() {
        if (instance == null) {
            instance = new Runtime();
        }
        return instance;
    }

    public void start() {
        logger.info("Starting CommandBridge");
        onScript.copyDefaultScript("bukkit-example.yml", "example.yml");
        onScript.loadAllScripts();
        client.connect(config.getKey("config.yml", "remote"), Integer.parseInt(config.getKey("config.yml", "port")));
        VersionChecker.setProjectId("wIuI4ru2");
        checkForUpdates();
    }

    public void stop() {
        logger.info("Stopping CommandBridge");
        client.disconnect();
    }

    private void checkForUpdates() {
        String currentVersion = Main.getVersion();
        logger.info("Checking for updates...");

        new Thread(() -> {
            String latestVersion = VersionChecker.getLatestVersion();

            if (latestVersion == null) {
                logger.info("Unable to check for updates");
                return;
            }

            if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                logger.warn("A new version is available: {}", latestVersion);
                logger.warn("Please download the latest release: {}", VersionChecker.getDownloadUrl());
            } else {
                logger.debug("You are running the latest version: {}", currentVersion);
            }
        }).start();
    }

    public ConfigManager getConfig() {
        return config;
    }

    public onScript getScript() {
        return onScript;
    }

    public Logger getLogger() {
        return logger;
    }
}
