package dev.consti.bukkit;

import client.ClientProvider;
import utils.ConfigProvider;
import utils.Logger;

public class BukkitStartup {
    private final ConfigProvider config;
    private final ClientProvider clientProvider;
    private final Logger logger;

    public BukkitStartup(ConfigProvider config, ClientProvider clientProvider, Logger logger) {
        this.config = config;
        this.clientProvider = clientProvider;
        this.logger = logger;
    } 

    public void start() {
        config.loadConfig("bukkit-config.yml", "config.yml");
        logger.setDebug(Boolean.parseBoolean(config.getKey("debug")));
        logger.info("Starting SocketLib");
        logger.debug("Debug mode enabled");
        clientProvider.connect(config.getKey("remote"), Integer.parseInt(config.getKey("port")), config.getKey("secret"));
    }

    public void stop() {
        logger.info("Stopping SocketLib");
        clientProvider.disconnect();
    }
}
