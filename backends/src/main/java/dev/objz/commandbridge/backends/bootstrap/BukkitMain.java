package dev.objz.commandbridge.backends.bootstrap;

import dev.objz.commandbridge.backends.loader.PlatformDetector;
import dev.objz.commandbridge.backends.loader.PlatformDetector.Platform;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.logging.Log;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class BukkitMain extends JavaPlugin {

	private PlatformAdapter adapter;

	@Override
	public void onLoad() {
		// Install SLF4J logger bridge once (Paper/Spigot provide one)
		try {
			org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("CommandBridge");
			Log.install(logger);
		} catch (Throwable t) {
			// fallback to Bukkit logger if needed
			getLogger().warning("Failed to install SLF4J logger, continuing with Bukkit logger: " + t);
		}

		// Shared, platform-agnostic initialization hook (can grow later)
		Log.info("Initializing CommandBridge (Bukkit family)...");
	}

	@Override
	public void onEnable() {
		try {
			// Detect actual runtime (Folia / Paper / Bukkit)
			Platform platform = PlatformDetector.detectPlatform(); // :contentReference[oaicite:0]{index=0}
			Log.info("Detected platform: {}", platform);

			// Choose adapter class
			switch (platform) {
				case FOLIA -> adapter = new dev.objz.commandbridge.folia.impl.Adapter();
				case PAPER -> adapter = new dev.objz.commandbridge.paper.impl.Adapter();
				case BUKKIT, UNKNOWN -> adapter = new dev.objz.commandbridge.bukkit.impl.Adapter();
				default -> {
					Log.warn("Non-Bukkit platform detected ({}). Falling back to Bukkit adapter.",
							platform);
					adapter = new dev.objz.commandbridge.bukkit.impl.Adapter();
				}
			}

			// Prepare environment
			Path dataDir = getDataFolder().toPath();
			PlatformAdapter.PlatformEnv env = new PlatformAdapter.PlatformEnv(dataDir);

			// Start adapter
			adapter.start(env);
			Log.success(true, "CommandBridge backend started");
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
				Log.success(true, "CommandBridge backend stopped");
			}
		} catch (Exception ex) {
			Log.error(ex, "Error during shutdown");
		}
	}
}
