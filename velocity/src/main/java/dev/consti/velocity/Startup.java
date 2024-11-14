package dev.consti.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.logging.Logger;
import dev.consti.utils.ConfigManager;
import dev.consti.velocity.websocket.Server;
import org.slf4j.LoggerFactory;


public class Startup {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Startup.class);
    private final Logger logger;
    private final ConfigManager config;
    private final Server server;


    public Startup() {
        this.logger = new Logger();
        this.config = new ConfigManager(logger, "CommandBridge");
        this.server = new Server(logger, config.getSecret());
    }

    public void start() {
        logger.info("Starting CommandBridge");
        String configName = "config.yml";
        logger.setDebug(Boolean.parseBoolean(config.getKey(configName, "debug")));
        config.loadConfig("velocity-config.yml", configName);
        config.loadSecret();
        server.startServer(Integer.parseInt(config.getKey(configName, "port")), config.getKey(configName, "host"));
    }

    public void stop() {
        logger.info("Stopping SocketLib");
        server.stopServer(Integer.parseInt(config.getKey("config.yml", "timeout")));
    }



}
