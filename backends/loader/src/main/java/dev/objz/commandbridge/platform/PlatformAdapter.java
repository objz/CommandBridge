package dev.objz.commandbridge.platform;

import java.nio.file.Path;

public interface PlatformAdapter {
	void start(PlatformEnv env) throws Exception;

	void stop() throws Exception;

	record PlatformEnv(String platformName, Path dataDir) {
	}
}
