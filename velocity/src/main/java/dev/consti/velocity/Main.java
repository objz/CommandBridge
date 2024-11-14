package dev.consti;

import javax.inject.Inject;


import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import server.ServerProvider;
import server.WebSocketServerImpl;
import utils.ConfigManager;
import utils.ConfigProvider;


@Plugin(id = "commandbridge", name = "CommandBridge", version = "1.0.0", authors = "72-S")
public class VelocityMain{
    private final ProxyServer server;
    private VelocityStartup startup;


    public VelocityMain(ProxyServer server) {
        this.server = server;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        startup.start();
    }    
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        startup.stop();
    }


}
