package dev.consti.commandbridge.velocity.core;


import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.websocket.Server;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.VersionChecker;

public class Startup {
    private final Logger logger;
    private final Runtime runtime;
    private boolean placeholderAPI = false;

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
                    runtime.getConfig().getKey("config.yml", "host"),
                    runtime.getConfig().getKey("config.yml", "san")
            );

            logger.debug("Setting up version checker...");
            VersionChecker.setProjectId("wIuI4ru2");

            logger.debug("Checking for updates...");
            checkForUpdates();

            logger.debug("Registering internal commands...");
            runtime.getGeneralUtils().registerCommands();

            if (Main.getInstance().proxy.getPluginManager().getPlugin("papiproxybridge").isPresent()) {
                logger.info("Hooked into PapiProxyBridge — PlaceholderAPI placeholders enabled");
                placeholderAPI = true;
            } else {
                logger.warn("PapiProxyBridge not found — using internal placeholder system only");
                placeholderAPI = false;
            }
        } catch (Exception e) {
            logger.error("Failed to initialize CommandBridge: {}",
                    logger.getDebug() ? e : e.getMessage()
            );
        }
    }

    public boolean isPlaceholderAPI() {
        return placeholderAPI;
    }

    public void stop() {
        try {
            Server server = runtime.getServer();
            logger.debug("Stopping WebSocket server...");
            for (String conn : server.getConnectedClients()) {
                runtime.getServer().sendTask(server.getWebSocket(conn), "reconnect", "closed");
            }
            runtime.getServer().stopServer(0);
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
