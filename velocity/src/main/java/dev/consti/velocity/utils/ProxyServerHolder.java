package dev.consti.velocity.utils;

import com.velocitypowered.api.proxy.ProxyServer;

public class ProxyServerHolder {
    private static ProxyServer proxyServer;

    private ProxyServerHolder(){};

    public static void setProxyServer(ProxyServer server){
        if (proxyServer == null) {
            proxyServer = server;
        } else {
            throw new IllegalStateException("Proxy instance is already set!");
        }
    }

    public static ProxyServer getProxyServer() {
        if (proxyServer == null) {
            throw new IllegalStateException("Proxy instance is not initalized!");
        }
        return proxyServer;
    }
}
