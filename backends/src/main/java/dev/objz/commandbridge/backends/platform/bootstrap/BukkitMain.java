package dev.objz.commandbridge.backends.platform.bootstrap;

import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.PlatformDetector;
import dev.objz.commandbridge.backends.platform.PlatformDetector.Platform;
import dev.objz.commandbridge.logging.Log;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitMain extends JavaPlugin {

    private PlatformAdapter<JavaPlugin> adapter;

    @Override
    public void onLoad() {
        try {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("commandbridge");
            Log.install(logger);
        } catch (Throwable t) {
            Log.install(getLogger());
        }

        Log.info("Initializing CommandBridge (Bukkit family)");

        Platform platform = PlatformDetector.detectPlatform();
        Log.info("Detected platform: {}", platform);
        try {
            adapter = switch (platform) {
                case FOLIA -> loadAdapter("dev.objz.commandbridge.folia.Adapter");
                case PAPER -> loadAdapter("dev.objz.commandbridge.paper.Adapter");
                case BUKKIT -> loadAdapter("dev.objz.commandbridge.bukkit.Adapter");
                default -> {
                    Log.warn("Unknown platform detected ({}). Falling back to Bukkit adapter",
                            platform);
                    yield loadAdapter("dev.objz.commandbridge.bukkit.impl.Adapter");
                }
            };
            var env = new PlatformAdapter.PlatformEnv(getDataFolder().toPath());
            adapter.load(env, this);
        } catch (Exception e) {
            Log.error("Adapter load failed during onLoad: {}", e.getMessage());
        }

    }

    @Override
    public void onEnable() {
        int pluginID = 22008;
        new Metrics(this, pluginID);
        try {
            var env = new PlatformAdapter.PlatformEnv(getDataFolder().toPath());
            adapter.start(env);
        } catch (Exception ex) {
            Log.error("Failed to enable CommandBridge: {}", ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (adapter != null) {
                adapter.stop();
            }
        } catch (Exception ex) {
            Log.error("Error during shutdown: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private PlatformAdapter<JavaPlugin> loadAdapter(String className) throws Exception {
        try {
            Class<?> clazz = Class.forName(className);
            return (PlatformAdapter<JavaPlugin>) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Adapter class not found: " + className +
                    ". Make sure the corresponding backend module is included in the build.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load adapter: " + className, e);
        }
    }
}
