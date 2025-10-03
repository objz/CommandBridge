package dev.objz.commandbridge.bootstrap;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.platform.PlatformAdapter;
import dev.objz.commandbridge.platform.PlatformDetector;
import dev.objz.commandbridge.platform.PlatformLauncher;
import org.bukkit.plugin.java.JavaPlugin;
public final class BukkitMain extends JavaPlugin {

	@Override
	public void onLoad() {
		CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(false).silentLogs(false).skipReloadDatapacks(true).shouldHookPaperReload(true));
	}

	@Override
	public void onEnable() {
		Log.install(getLogger());
		var env = new PlatformAdapter.PlatformEnv(
				PlatformDetector.detectPlatform().name(),
				getDataFolder().toPath());
		CommandAPI.onEnable();
		PlatformLauncher.start(env);
	}

	@Override
	public void onDisable() {
		var env = new PlatformAdapter.PlatformEnv(
				PlatformDetector.detectPlatform().name(),
				getDataFolder().toPath());
		CommandAPI.onDisable();
		PlatformLauncher.stop(env);
	}
}
