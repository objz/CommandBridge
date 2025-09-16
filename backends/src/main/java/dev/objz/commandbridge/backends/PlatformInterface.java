package dev.objz.commandbridge.backends;

import dev.objz.commandbridge.backends.api.PlatformRegistry;

public interface PlatformInterface {
	void enable();
	void disable();
	PlatformRegistry platformRegistry();
}
