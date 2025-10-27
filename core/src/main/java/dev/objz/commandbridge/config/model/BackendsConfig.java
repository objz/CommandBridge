package dev.objz.commandbridge.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record BackendsConfig(
		@Setting("host") String host,
		@Setting("port") int port,
		@Setting("client-id") String clientId,
		@Setting("security") Security security,
		@Setting("timeouts") Timeouts timeouts,
		@Setting("limits") Limits limits,
		@Setting("debug") boolean debug) {
	@ConfigSerializable
	public static record Security(
			@Setting("tls-mode") TlsMode tlsMode,
			@Setting("tls-pin") String tlsPin,
			@Setting("secret") String secret,
			@Setting("require-auth") Boolean requireAuth) {
	}

	@ConfigSerializable
	public static record Timeouts(
			@Setting("auth-timeout") int authTimeout) {
	}

	@ConfigSerializable
	public static record Limits(
			@Setting("inbound-messages-per-sec") int inboundMessagesSec) {
	}

	public static BackendsConfig defaults() {
		return new BackendsConfig(
				"127.0.0.1",
				8765,
				"survival-1",
				new Security(TlsMode.TOFU, "", "change-me", true),
				new Timeouts(5),
				new Limits(60),
				false);
	}
}
