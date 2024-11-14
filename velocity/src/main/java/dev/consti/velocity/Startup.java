package dev.consti;

import com.velocitypowered.api.proxy.ProxyServer;

import server.ServerProvider;
import utils.ConfigProvider;
import utils.Logger;

public class VelocityStartup {
    private final ProxyServer server;
    private final ConfigProvider config;
    private final ServerProvider serverProvider;
    private final Logger logger;

    public VelocityStartup(ProxyServer server, ConfigProvider config, Logger logger, ServerProvider serverProvider) {
        this.server = server; 
        this.config = config;
        this.serverProvider = serverProvider;
        this.logger = logger;

    }

    public void start() {
        config.loadConfig("velocity-config.yml", "config.yml");
        config.loadSecret("secret.key");
        logger.setDebug(Boolean.parseBoolean(config.getKey("debug")));
        logger.info("Starting SocketLib");
        logger.debug("Debug mode enabled");
        serverProvider.startServer(Integer.parseInt(config.getKey("port")), config.getKey("host"), config.getSecret());

    }

    public void stop() {
        logger.info("Stopping SocketLib");
        serverProvider.stopServer(Integer.parseInt(config.getKey("delay")));
    }



}
