package dev.objz.commandbridge.backends;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
	private PlatformInterface platform;

	@Override
	public void onEnable() {
		this.platform = PlatformResolver.detect(this);
		platform.enable();
	}

	@Override
	public void onDisable() {
		if (platform != null) platform.disable();
	}
}
