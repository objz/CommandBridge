package dev.consti.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import client.ClientProvider;
import client.WebSocketClientImpl;
import utils.ConfigManager;
import utils.ConfigProvider;

public class BukkitMain extends JavaPlugin {
    WebSocketClientImpl webSocketClient;
    private BukkitStartup startup;

    public BukkitMain() {
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
