package dev.consti.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private final Startup startup = Startup.getInstance();

    @Override
    public void onEnable() {
        startup.start();
    }

    @Override
    public void onDisable() {
        startup.stop();
    }

    public static String getVersion() {
        return "2.0.0";
    }

}
