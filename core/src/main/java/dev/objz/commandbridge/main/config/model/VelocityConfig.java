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
			@Setting("auth-timeout-seconds") int authTimeoutSeconds) {
	}

	@ConfigSerializable
	public static record Network() {
	}

	@ConfigSerializable
	public static record Limits(
			@Setting("max-connections") int maxConnections,
			@Setting("max-message-size-bytes") int maxMessageSizeBytes) {
	}

	public static VelocityConfig defaults() {
		return new VelocityConfig(
				"0.0.0.0",
				8765,
				"proxy-1",
				new Heartbeat(10, 60),
				new Security(true, 10),
				new Limits(100, 65_536),
				false);
	}
}
