package dev.objz.commandbridge.main.config.profile;

import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;

import java.util.Set;

public final class BackendsConfigProfile implements ConfigProfile<BackendsConfig> {

	@Override
	public BackendsConfig defaults() {
		return BackendsConfig.defaults();
	}

	@Override
	public Set<String> validKeys() {
		return Set.of("host", "port", "tls", "client-id", "secret", "debug");
	}

	@Override
	public void validate(BackendsConfig cfg) {
		if (cfg.port() <= 0 || cfg.port() > 65535) {
			Log.error("port must be between 1 and 65535");
		}
		if (cfg.host() == null || cfg.host().isBlank()) {
			Log.error("host must not be empty");
		}
		if (cfg.host() != null && (cfg.host().startsWith("ws://") || cfg.host().startsWith("wss://"))) {
			Log.warn("host contains a ws:// or wss:// scheme. This is supported for compatibility but deprecated. Remove the scheme and use the 'tls' boolean instead");
		}
		if (cfg.clientId() == null || cfg.clientId().isBlank()) {
			Log.error("client-id must not be empty");
		}
		if (cfg.secret() == null || cfg.secret().isBlank()) {
			Log.error("secret must not be empty");
		}
		if ("change-me".equals(cfg.secret())) {
			Log.warn("Update 'secret' in config.yml using the key from secret.key on your Velocity server");
		}
	}
}
