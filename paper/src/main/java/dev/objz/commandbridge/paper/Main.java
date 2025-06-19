package dev.objz.commandbridge.paper;

import java.io.InputStream;
import java.util.Properties;

import org.bukkit.plugin.java.JavaPlugin;

import dev.objz.commandbridge.paper.core.Runtime;
import dev.objz.commandbridge.core.Logger;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

public class Main extends JavaPlugin {
    private static Main instance;
    private final Logger logger;

    public Main() {
        instance = this;
        logger = Runtime.getInstance().getLogger();
    }

    public static String getVersion() {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("plugin.properties")) {
            if (input == null) {
                return "Unknown";
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("plugin.version", "Unknown");
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(false).silentLogs(true)
                .skipReloadDatapacks(true).shouldHookPaperReload(false));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        logger.info("Initializing CommandBridge...");
        Runtime.getInstance().getStartup().start();
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        logger.info("Stopping CommandBridge...");
        Runtime.getInstance().getStartup().stop();
    }

}
