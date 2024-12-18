package dev.consti.commandbridge.velocity.util;

import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.logging.Logger;

public class ProxyUtils {
    private static ProxyServer proxyServer;
    private static final Logger logger = Runtime.getInstance().getLogger();

    private ProxyUtils() {
    }

    public static synchronized void setProxyServer(ProxyServer server) {
        if (proxyServer == null) {
            proxyServer = server;
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
        return proxyServer;
    }
}
