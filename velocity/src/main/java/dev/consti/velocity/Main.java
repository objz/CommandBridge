package dev.consti.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;


@Plugin(id = "commandbridge", name = "CommandBridge", version = "2.0.0", authors = "72-S")
public class Main{
    private Startup startup;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        startup.start();
    }    
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        startup.stop();
    }


}
