package dev.objz.commandbridge.main.config.profile;

import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.config.model.BackendsConfig.TlsMode;
import dev.objz.commandbridge.main.logging.Log;

import java.util.Set;

public final class BackendsConfigProfile implements ConfigProfile<BackendsConfig> {

	@Override
	public BackendsConfig defaults() {
		return BackendsConfig.defaults();
	}

	@Override
	public Set<String> validKeys() {
		return Set.of(
				"host",
				"port",
				"tls-mode",
				"tls-pin",
				"client-id",
				"secret",
				"debug");
	}

	@Override
	public boolean validate(BackendsConfig cfg) {
		boolean ok = true;
		if (cfg.port() <= 0 || cfg.port() > 65535) {
			Log.error("port must be between 1 and 65535");
			ok = false;
		}
		if (cfg.host() == null || cfg.host().isBlank()) {
			Log.error("host must not be empty");
			ok = false;
		}
		if (cfg.host() != null && (cfg.host().startsWith("ws://") || cfg.host().startsWith("wss://"))) {
			Log.error("host must NOT include ws:// or wss:// (remove scheme; use 'tls-mode' to control TLS)");
			ok = false;
		}
		if (cfg.clientId() == null || cfg.clientId().isBlank()) {
			Log.error("client-id must not be empty");
			ok = false;
		}
		if (cfg.secret() == null || cfg.secret().isBlank()) {
			Log.error("secret must not be empty");
			ok = false;
		}
		if ("change-me".equals(cfg.secret())) {
			Log.warn("Update 'secret' in config.yml using the key from secret.key on your Velocity server");
			ok = false;
		}

		TlsMode mode = cfg.effectiveTlsMode();
		if (mode == TlsMode.PLAINTEXT) {
			Log.warn("TLS is disabled (PLAINTEXT). This is not recommended in production.");
		}
		return ok;
	}
}
