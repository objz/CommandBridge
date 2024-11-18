package dev.consti.velocity;



import dev.consti.logging.Logger;
import dev.consti.utils.ConfigManager;
import dev.consti.utils.VersionChecker;
import dev.consti.velocity.utils.onScript;
import dev.consti.velocity.websocket.Server;


public class Runtime {
    private static Runtime instance;
    private final Logger logger;
    private final ConfigManager config;
    private final onScript onScript;
    private final Server server;


    private Runtime() {
        this.logger = new Logger("CommandBridge");
        this.config = new ConfigManager(logger, "CommandBridge");
        this.onScript = new onScript(logger, "CommandBridge");
        config.copyConfig("velocity-config.yml", "config.yml");
        config.loadAllConfigs();
        config.loadSecret();
        logger.setDebug(Boolean.parseBoolean(config.getKey("config.yml", "debug")));
        this.server = new Server(logger, config.getSecret());
    }

    public static synchronized Runtime getInstance() {
        if (instance == null) {
            instance = new Runtime();
        }
        return instance;
    }

    public void start() {
        logger.info("Starting CommandBridge");
        onScript.copyDefaultScript("velocity-example.yml", "example.yml");
        onScript.loadAllScripts();
        server.startServer(Integer.parseInt(config.getKey("config.yml", "port")), config.getKey("config.yml", "host"));
        VersionChecker.setProjectId("wIuI4ru2");
        checkForUpdates();
    }

    public void stop() {
        logger.info("Stopping SocketLib");
        server.stopServer(Integer.parseInt(config.getKey("config.yml", "timeout")));
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

    public Server getServer() { return server; }

}
