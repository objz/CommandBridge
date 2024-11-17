package dev.consti.velocity;



import dev.consti.logging.Logger;
import dev.consti.utils.ConfigManager;
import dev.consti.utils.VersionChecker;
import dev.consti.velocity.utils.Script;
import dev.consti.velocity.websocket.Server;


public class Startup {
    private static Startup instance;
    private final Logger logger;
    private final ConfigManager config;
    private final Script script;
    private final Server server;


    private Startup() {
        this.logger = new Logger("CommandBridge");
        this.config = new ConfigManager(logger, "CommandBridge");
        this.script = new Script(logger, "CommandBridge");
        config.copyConfig("velocity-config.yml", "config.yml");
        config.loadAllConfigs();
        config.loadSecret();
        logger.setDebug(Boolean.parseBoolean(config.getKey("config.yml", "debug")));
        this.server = new Server(logger, config.getSecret());
    }

    public static synchronized Startup getInstance() {
        if (instance == null) {
            instance = new Startup();
        }
        return instance;
    }

    public void start() {
        logger.info("Starting CommandBridge");
        script.copyDefaultScript("velocity-example.yml", "example.yml");
        script.loadAllScripts();
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

    public Script getScript() {
        return script;
    }

    public Logger getLogger() {
        return logger;
    }

}
