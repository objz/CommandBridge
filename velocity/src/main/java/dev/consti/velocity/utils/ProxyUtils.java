package dev.consti.velocity.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.logging.Logger;

public class ProxyUtils {
    private static ProxyServer proxyServer;
    private static final Logger logger = new Logger("ProxyUtils");

    private ProxyUtils() {
        logger.debug("ProxyUtils initialized.");
    }

    public static synchronized void setProxyServer(ProxyServer server) {
        if (proxyServer == null) {
            proxyServer = server;
            logger.info("ProxyServer instance set successfully.");
        } else {
            logger.error("Attempted to set ProxyServer instance more than once!");
            throw new IllegalStateException("Proxy instance is already set!");
        }
    }

    public static synchronized ProxyServer getProxyServer() {
        if (proxyServer == null) {
            logger.error("Attempted to retrieve ProxyServer instance before initialization!");
            throw new IllegalStateException("Proxy instance is not initialized!");
        }
        logger.debug("ProxyServer instance retrieved successfully.");
        return proxyServer;
    }
}
