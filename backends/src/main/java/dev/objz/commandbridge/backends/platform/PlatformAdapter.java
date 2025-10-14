package dev.objz.commandbridge.backends.platform;

import java.nio.file.Path;

public interface PlatformAdapter {
	record PlatformEnv(Path dataDir) {
	}

	default void load(PlatformEnv env) throws Exception {
	}

	void start(PlatformEnv env) throws Exception;

	void stop() throws Exception;
}
