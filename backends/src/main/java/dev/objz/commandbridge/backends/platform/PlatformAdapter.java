package dev.objz.commandbridge.backends.platform;

import java.nio.file.Path;

import org.bukkit.plugin.java.JavaPlugin;

public interface PlatformAdapter {
	record PlatformEnv(Path dataDir) {
	}

	default void load(PlatformEnv env, JavaPlugin plugin) throws Exception {
	}

	void start(PlatformEnv env) throws Exception;

	void stop() throws Exception;
}
