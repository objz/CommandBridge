package dev.objz.commandbridge.main.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record VelocityConfig(
		@Setting("bind-host") String bindHost,
		@Setting("bind-port") int bindPort,
		@Setting("server-id") String serverId,
		@Setting("heartbeat") Heartbeat heartbeat,
		@Setting("security") Security security,
		@Setting("timeouts") Timeouts timeouts,
		@Setting("limits") Limits limits,
		@Setting("debug") boolean debug) {
	@ConfigSerializable
	public static record Heartbeat(
			@Setting("app-ping-seconds") int appPingSeconds,
			@Setting("stale-after-seconds") int staleAfterSeconds) {
	}

	@ConfigSerializable
	public static record Security(
			@Setting("require-auth") boolean requireAuth,
			@Setting("auth-timeout-seconds") int authTimeoutSeconds,
			@Setting("tls-mode") TlsMode tlsMode,
			@Setting("keystore-path") String keystorePath,
			@Setting("keystore-password") String keystorePassword,
			@Setting("keystore-type") String keystoreType // PKCS12 | JKS
	) {
	}

	@ConfigSerializable
	public static record Timeouts(
			@Setting("register-timout") int registerTimeout) {
	}

	@ConfigSerializable
	public static record Limits(
			@Setting("inbound-messages-per-sec") int inboundMessagesSec,
			@Setting("max-connections") int maxConnections,
			@Setting("max-message-size-bytes") int maxMessageSizeBytes) {
	}

	public static VelocityConfig defaults() {
		return new VelocityConfig(
				"0.0.0.0",
				8765,
				"proxy-1",
				new Heartbeat(10, 60),
				new Security(true, 10, TlsMode.TOFU, "", "", "PKCS12"),
				new Timeouts(5),
				new Limits(60, 100, 65_536),
				false);
	}
}
