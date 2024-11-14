package dev.consti.bukkit;

import dev.consti.bukkit.websocket.Client;
import dev.consti.utils.ConfigManager;
import dev.consti.logging.Logger;

public class Startup {
    private final Logger logger;
    private final ConfigManager config;
    private final Client client;

    public Startup() {
        this.logger = new Logger();
        this.config = new ConfigManager(logger, "CommandBridge");
        this.client = new Client(logger, config.getSecret());
    } 

    public void start() {
        logger.info("Starting CommandBridge");
        String configName = "config.yml";
        logger.setDebug(Boolean.parseBoolean(config.getKey(configName, "debug")));
        config.loadConfig("bukkit-config.yml", configName);
        config.loadSecret();
        client.connect(config.getKey(configName, "remote"), Integer.parseInt(config.getKey(configName, "port")));
    }

    public void stop() {
        logger.info("Stopping CommandBridge");
        client.disconnect();
    }
}
