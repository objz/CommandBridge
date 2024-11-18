package dev.consti.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private final Runtime runtime = Runtime.getInstance();

    @Override
    public void onEnable() {
        runtime.start();
    }

    @Override
    public void onDisable() {
        runtime.stop();
    }

    public static String getVersion() {
        return "2.0.0";
    }

}
