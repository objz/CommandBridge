package dev.consti.bukkit;

import dev.consti.bukkit.core.Runtime;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private final dev.consti.bukkit.core.Runtime runtime = Runtime.getInstance();

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
    }

    public static String getVersion() {
        return "2.0.0";
    }

}
