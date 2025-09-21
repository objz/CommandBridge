package dev.objz.commandbridge.bootstrap;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.platform.PlatformAdapter;
import dev.objz.commandbridge.platform.PlatformDetector;
import dev.objz.commandbridge.platform.PlatformLauncher;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitMain extends JavaPlugin {
	@Override
	public void onEnable() {
		Log.install(org.slf4j.LoggerFactory.getLogger(getLogger().getName()));
		var env = new PlatformAdapter.PlatformEnv(
				PlatformDetector.detectPlatform().name(),
				getDataFolder().toPath());
		PlatformLauncher.start(env);
	}

	@Override
	public void onDisable() {
		var env = new PlatformAdapter.PlatformEnv(
				PlatformDetector.detectPlatform().name(),
				getDataFolder().toPath());
		PlatformLauncher.stop(env);
	}
}
