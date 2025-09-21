package dev.objz.commandbridge.main.config.profile;

import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.config.model.TlsMode;
import dev.objz.commandbridge.main.logging.Log;


public final class BackendsConfigProfile implements ConfigProfile<BackendsConfig> {
	@Override
	public BackendsConfig defaults() {
		return BackendsConfig.defaults();
	}

	@Override
	public Result<BackendsConfig> normalize(BackendsConfig in) {
		BackendsConfig d = defaults();
		boolean ok = true;

		String host = in.host();
		if (host == null || host.isBlank()) {
			Log.error("'host' must not be empty");
			host = d.host();
			ok = false;
		} else {
			host = host.trim();
			if (host.startsWith("ws://") || host.startsWith("wss://")) {
				Log.warn("'host' must NOT include ws:// or wss:// ");
				host = host.replaceFirst("^wss?://", ""); // normalize anyway
			}
		}

		// port
		int port = in.port();
		if (port <= 0 || port > 65535) {
			Log.error("'port' must be between 1 and 65535");
			port = d.port();
			ok = false;
		}

		// client-id
		String clientId = in.clientId();
		if (clientId == null || clientId.isBlank()) {
			Log.warn("'client-id' missing or blank");
			clientId = d.clientId();
			ok = false;
		}

		// limits
		BackendsConfig.Limits limitsIn = in.limits();
		int inboundMessagesSec = (limitsIn != null ? limitsIn.inboundMessagesSec()
				: d.limits().inboundMessagesSec());
		if (inboundMessagesSec <= 0) {
			Log.error("'limits.inbound-messages-per-sec' must be positive");
			inboundMessagesSec = d.limits().inboundMessagesSec();
			ok = false;
		}
		BackendsConfig.Limits limitsOut = new BackendsConfig.Limits(inboundMessagesSec);

		// security
		BackendsConfig.Security secIn = in.security();
		BackendsConfig.Security secOut;

		// tls-mode
		TlsMode tlsMode = (secIn.tlsMode() != null) ? secIn.tlsMode() : d.security().tlsMode();
		if (secIn.tlsMode() == null) {
			Log.error("'security.tls-mode' must be set");
			ok = false;
		}

		// tls-pin
		String tlsPin = (secIn.tlsPin() != null && !secIn.tlsPin().isBlank()) ? secIn.tlsPin().trim()
				: d.security().tlsPin();

		// secret
		String secret = secIn.secret();
		if (secret == null || secret.isBlank()) {
			Log.error("'security.secret' must not be empty");
			secret = d.security().secret();
			ok = false;
		} else if (secret.toLowerCase().contains("change-me")) {
			Log.warn("'security.secret' contains 'change-me'. replace with real key");
		}

		// require-auth
		Boolean requireAuth = secIn.requireAuth();
		if (requireAuth == null) {
			Log.warn("'security.require-auth' must be set");
			requireAuth = d.security().requireAuth();
		} else if (Boolean.FALSE.equals(requireAuth)) {
			Log.warn("Authentication is disabled! This is insecure and should not be used");
		}

		if (tlsMode == TlsMode.PLAIN && Boolean.TRUE.equals(requireAuth)) {
			Log.warn("'tls-mode=PLAIN' with 'require-auth=true' is unusual. consider TLS");
		}

		secOut = new BackendsConfig.Security(tlsMode, tlsPin, secret, requireAuth);

		BackendsConfig out = new BackendsConfig(host, port, clientId, secOut, limitsOut, in.debug());
		return new Result<>(out, ok);
	}
}
