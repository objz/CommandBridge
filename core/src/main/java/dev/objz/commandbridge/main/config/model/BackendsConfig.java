package dev.objz.commandbridge.main.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.net.URI;

@ConfigSerializable
public record BackendsConfig(
		@Setting("host") String host,
		@Setting("port") int port,
		@Setting("client-id") String clientId,
		@Setting("secret") String secret,
		@Setting("debug") boolean debug) {
	public static BackendsConfig defaults() {
		return new BackendsConfig(
				"ws://127.0.0.1",
				8765,
				"survival-1",
				"change-me",
				false);
	}

	public URI uri() {
		return URI.create(host + ":" + port + "/ws");
	}
}
