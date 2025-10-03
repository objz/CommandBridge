package dev.objz.commandbridge.config.profile;

import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;


public final class VelocityConfigProfile implements ConfigProfile<VelocityConfig> {
	@Override
	public VelocityConfig defaults() {
		return VelocityConfig.defaults();
	}

	@Override
	public Result<VelocityConfig> normalize(VelocityConfig in) {
		VelocityConfig d = defaults();
		boolean ok = true;

		// bind-host
		String bindHost = in.bindHost();
		if (bindHost == null || bindHost.isBlank()) {
			Log.error("'bind-host' must not be empty");
			bindHost = d.bindHost();
			ok = false;
		} else {
			bindHost = bindHost.trim();
			if (bindHost.startsWith("ws://") || bindHost.startsWith("wss://")) {
				Log.warn("'bind-host' must NOT include ws:// or wss://");
				bindHost = bindHost.replaceFirst("^wss?://", "");
			}
		}

		// bind-port
		int bindPort = in.bindPort();
		if (bindPort <= 0 || bindPort > 65535) {
			Log.error("'bind-port' must be between 1 and 65535");
			bindPort = d.bindPort();
			ok = false;
		}

		// server-id
		String serverId = in.serverId();
		if (serverId == null || serverId.isBlank()) {
			serverId = d.serverId();
			Log.warn("'server-id' missing or blank. defaulting to '{}'", serverId);
		} else {
			serverId = serverId.trim();
		}

		// heartbeat
		VelocityConfig.Heartbeat hb = in.heartbeat();
		int appPing = hb.appPingSeconds();
		int stale = hb.staleAfterSeconds();
		if (appPing <= 0) {
			Log.error("'app-ping-seconds' must be > 0");
			appPing = d.heartbeat().appPingSeconds();
			ok = false;
		}
		if (stale < appPing) {
			Log.error("'stale-after-seconds' must be >= 'app-ping-seconds'");
			stale = Math.max(appPing, d.heartbeat().staleAfterSeconds());
			ok = false;
		}
		hb = new VelocityConfig.Heartbeat(appPing, stale);

		VelocityConfig.Timeouts to = in.timeouts();
		int registerTimeout = to.registerTimeout();
		if (registerTimeout <= 0) {
			Log.error("'timeouts.register-timout' must be > 0");
			registerTimeout = d.timeouts().registerTimeout();
			ok = false;
		}
		to = new VelocityConfig.Timeouts(registerTimeout);

		// limits
		VelocityConfig.Limits limits = in.limits();
		int maxCon = limits.maxConnections();
		int maxMsg = limits.maxMessageSizeBytes();
		int inboundPerSec = limits.inboundMessagesSec();
		if (maxCon <= 0) {
			Log.error("'max-connections' must be positive");
			maxCon = d.limits().maxConnections();
			ok = false;
		}
		if (maxMsg < 1024) {
			Log.error("'max-message-size-bytes' too small");
			maxMsg = Math.max(1024, d.limits().maxMessageSizeBytes());
			ok = false;
		}
		if (inboundPerSec <= 0) {
			Log.error("'inbound-messages-per-sec' must be positive");
			inboundPerSec = d.limits().inboundMessagesSec();
			ok = false;
		}
		limits = new VelocityConfig.Limits(inboundPerSec, maxCon, maxMsg);

		// security
		VelocityConfig.Security secIn = in.security();

		TlsMode tlsMode = (secIn.tlsMode() != null) ? secIn.tlsMode() : d.security().tlsMode();
		if (secIn.tlsMode() == null) {
			Log.error("'security.tls-mode' must be set");
			ok = false;
		}

		boolean requireAuth = secIn.requireAuth(); // primitive
		int authTimeout = secIn.authTimeoutSeconds();

		String keystorePath = emptyToNull(secIn.keystorePath());
		String keystorePassword = secIn.keystorePassword(); // may be null
		String keystoreType = (secIn.keystoreType() == null || secIn.keystoreType().isBlank())
				? d.security().keystoreType()
				: secIn.keystoreType().trim();

		if (!requireAuth) {
			Log.warn("Authentication is disabled! This is insecure and should not be used");
		}
		if (tlsMode == TlsMode.PLAIN && requireAuth) {
			Log.warn("'tls-mode=PLAIN' with 'require-auth=true' is unusual. consider TLS");
		}

		// STRICT requires keystore pieces
		if (tlsMode == TlsMode.STRICT) {
			if (keystorePath == null || keystorePath.isBlank()) {
				Log.error("'security.keystore-path' is required in STRICT mode");
				ok = false;
				keystorePath = d.security().keystorePath();
			}
			if (keystorePassword == null) {
				Log.error("'security.keystore-password' is required in STRICT mode");
				ok = false;
				keystorePassword = d.security().keystorePassword();
			}
			if (keystoreType == null || keystoreType.isBlank()) {
				Log.error("security.keystore-type is required in STRICT mode");
				ok = false;
				keystoreType = d.security().keystoreType();
			}
		}

		VelocityConfig.Security secOut = new VelocityConfig.Security(
				requireAuth, authTimeout, tlsMode, keystorePath, keystorePassword, keystoreType);

		VelocityConfig out = new VelocityConfig(bindHost, bindPort, serverId, hb, secOut, to, limits,
				in.debug());
		return new Result<>(out, ok);
	}

	private static String emptyToNull(String s) {
		return (s != null && s.isBlank()) ? null : s;
	}
}
