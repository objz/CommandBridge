package dev.consti.bukkit;

import org.bukkit.plugin.java.JavaPlugin;



public class Main extends JavaPlugin {
    WebSocketClientImpl webSocketClient;
    private BukkitStartup startup;

    public Main() {
    }

    @Override
    public void onEnable() {
        startup.start();
    }

    @Override
    public void onDisable() {
        startup.stop();
    }

}
