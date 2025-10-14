package dev.objz.commandbridge.backends.bootstrap;

import dev.objz.commandbridge.backends.loader.PlatformDetector;
import dev.objz.commandbridge.backends.loader.PlatformDetector.Platform;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.logging.Log;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitMain extends JavaPlugin {

	private PlatformAdapter adapter;

	@Override
	public void onLoad() {
		try {
			org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("CommandBridge");
			Log.install(logger);
		} catch (Throwable t) {
			Log.install(getLogger());
		}

		Log.info("Initializing CommandBridge (Bukkit family)");

		Platform platform = PlatformDetector.detectPlatform();
		Log.info("Detected platform: {}", platform);
		try {
			adapter = switch (platform) {
				case FOLIA -> loadAdapter("dev.objz.commandbridge.folia.impl.Adapter");
				case PAPER -> loadAdapter("dev.objz.commandbridge.paper.impl.Adapter");
				case BUKKIT -> loadAdapter("dev.objz.commandbridge.bukkit.impl.Adapter");
				default -> {
					Log.warn("Unknown platform detected ({}). Falling back to Bukkit adapter.",
							platform);
					yield loadAdapter("dev.objz.commandbridge.bukkit.impl.Adapter");
				}
			};
			var env = new PlatformAdapter.PlatformEnv(getDataFolder().toPath());
			adapter.load(env);
		} catch (Exception e) {
			Log.error(e, "Adapter load failed during onLoad");
		}

	}

	@Override
	public void onEnable() {
		try {
			var env = new PlatformAdapter.PlatformEnv(getDataFolder().toPath());
			adapter.start(env);
		} catch (Exception ex) {
			Log.error(ex, "Failed to enable CommandBridge");
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
			Log.error(ex, "Error during shutdown");
		}
	}

	private PlatformAdapter loadAdapter(String className) throws Exception {
		try {
			Class<?> clazz = Class.forName(className);
			return (PlatformAdapter) clazz.getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Adapter class not found: " + className +
					". Make sure the corresponding backend module is included in the build.", e);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load adapter: " + className, e);
		}
	}
}
