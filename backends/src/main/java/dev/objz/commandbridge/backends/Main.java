package dev.objz.commandbridge.backends;

import org.bukkit.plugin.java.JavaPlugin;

import dev.objz.commandbridge.main.logging.Log;

public final class Main extends JavaPlugin {
	private PlatformInterface platform;

	@Override
	public void onEnable() {
		Log.install(org.slf4j.LoggerFactory.getLogger(getLogger().getName()));
		this.platform = PlatformResolver.detect(this);
		platform.enable();
	}

	@Override
	public void onDisable() {
		if (platform != null)
			platform.disable();
	}
}
