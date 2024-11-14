package dev.consti.bukkit;

import dev.consti.bukkit.utils.Script;
import dev.consti.bukkit.websocket.Client;
import dev.consti.utils.ConfigManager;
import dev.consti.logging.Logger;
import dev.consti.utils.VersionChecker;

public class Startup {
    private static Startup instance;
    private final Logger logger;
    private final ConfigManager config;
    private final Script script;
    private final Client client;

    private Startup() {
        this.logger = new Logger();
        this.config = new ConfigManager(logger, "CommandBridge");
        this.script = new Script(logger, "CommandBridge");
        config.copyConfig("bukkit-config.yml", "config.yml");
        config.loadAllConfigs();
        logger.setDebug(Boolean.parseBoolean(config.getKey("config.yml", "debug")));
        this.client = new Client(logger, config.getSecret());
    }

    public static synchronized Startup getInstance() {
        if (instance == null) {
            instance = new Startup();
        }
        return instance;
    }

    public void start() {
        logger.info("Starting CommandBridge");
        script.copyDefaultScript("bukkit-example.yml", "example.yml");
        script.loadAllScripts();
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

    public Script getScript() {
        return script;
    }

    public Logger getLogger() {
        return logger;
    }
}
