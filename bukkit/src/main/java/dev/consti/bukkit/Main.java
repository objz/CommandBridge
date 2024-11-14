package dev.consti.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private final Startup startup = new Startup();

    @Override
    public void onEnable() {
        startup.start();
    }

    @Override
    public void onDisable() {
        startup.stop();
    }

}
