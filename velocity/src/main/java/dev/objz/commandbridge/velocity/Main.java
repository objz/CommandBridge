package dev.objz.commandbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "commandbridge", name = "CommandBridge", version = "3.0.0",
        url = "https://cb.objz.dev", description = "I did it!", authors = {"objz"})

public final class Main {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public Main(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        logger.info("Hello from Velocity!");
    }
}
