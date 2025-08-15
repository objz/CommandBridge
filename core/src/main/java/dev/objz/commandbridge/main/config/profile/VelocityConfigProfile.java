package dev.objz.commandbridge.main.config.profile;

import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.logging.Log;

import java.util.Set;

public final class VelocityConfigProfile implements ConfigProfile<VelocityConfig> {

	@Override
	public VelocityConfig defaults() {
		return VelocityConfig.defaults();
	}

	@Override
	public Set<String> validKeys() {
		return Set.of("bind-host", "bind-port", "server-id", "heartbeat", "security", "network", "limits",
				"debug");
	}

	@Override
	public void validate(VelocityConfig cfg) {
		if (cfg.bindPort() <= 0 || cfg.bindPort() > 65535) {
			Log.error("bindPort must be between 1 and 65535");
		}
		if (cfg.limits().maxConnections() <= 0) {
			Log.error("maxConnections must be positive");
		}
		if (cfg.limits().maxMessageSizeBytes() < 1024) {
			Log.error("maxMessageSizeBytes too small");
		}
		if (cfg.heartbeat().appPingSeconds() <= 0) {
			Log.error("appPingSeconds must be > 0");
		}
		if (cfg.heartbeat().staleAfterSeconds() < cfg.heartbeat().appPingSeconds()) {
			Log.error("staleAfterSeconds must be >= appPingSeconds");
		}
	}
}
