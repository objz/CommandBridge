package dev.objz.commandbridge.backends.platform.bootstrap;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.logging.Log;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class VelocityMain {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final Object pluginInstance;

    private PlatformAdapter adapter;

    public VelocityMain(ProxyServer proxy, Logger logger, Path dataDirectory, Object pluginInstance) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginInstance = pluginInstance;
    }

    public void load() {
        try {
            String className = "dev.objz.commandbridge.velocity.Adapter";
            Class<?> clazz = Class.forName(className);
            this.adapter = (PlatformAdapter) clazz.getDeclaredConstructor().newInstance();

            var env = new PlatformAdapter.PlatformEnv(dataDirectory, "client.yml");
            adapter.load(env, this);

        } catch (ClassNotFoundException e) {
            Log.error("Adapter class not found: dev.objz.commandbridge.velocity.Adapter. " +
                    "Is the backends:velocity module loaded?", e);
        } catch (Exception e) {
            Log.error("Adapter load failed: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public void enable() {
        if (adapter == null)
            return;

        try {
            var env = new PlatformAdapter.PlatformEnv(dataDirectory, "client.yml");
            adapter.start(env);
        } catch (Exception ex) {
            Log.error("Failed to enable CommandBridge Backend: {}", ex.getMessage());
        }
    }

    public void disable() {
        if (adapter != null) {
            try {
                adapter.stop();
            } catch (Exception ex) {
                Log.error("Error during backend shutdown: {}", ex.getMessage());
            }
        }
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Object getPluginInstance() {
        return pluginInstance;
    }
}
