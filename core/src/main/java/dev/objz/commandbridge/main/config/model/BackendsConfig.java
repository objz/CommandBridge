package dev.objz.commandbridge.main.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.net.URI;

@ConfigSerializable
public record BackendsConfig(
		@Setting("host") String host,
		@Setting("port") int port,
		@Setting("tls-mode") TlsMode tlsMode,
		@Setting("tls-pin") String tlsPin,
		@Setting("client-id") String clientId,
		@Setting("secret") String secret,
		@Setting("debug") boolean debug) {

	public enum TlsMode {
		/** Plain WS */
		PLAINTEXT,
		/** WSS with TOFU pinning */
		TOFU,
		/** WSS with normal validation + hostname verification */
		STRICT
	}

	public static BackendsConfig defaults() {
		return new BackendsConfig(
				"127.0.0.1",
				8765,
				TlsMode.TOFU,
				"",
				"survival-1",
				"change-me",
				false);
	}

	public TlsMode effectiveTlsMode() {
		return (tlsMode != null) ? tlsMode : TlsMode.TOFU;
	}

	public boolean isTlsEnabled() {
		return effectiveTlsMode() != TlsMode.PLAINTEXT;
	}

	public boolean isStrict() {
		return effectiveTlsMode() == TlsMode.STRICT;
	}

	public URI uri() {
		String h = host != null ? host.trim() : "";
		if (h.isEmpty())
			throw new IllegalStateException("host must not be empty");
		String scheme = isTlsEnabled() ? "wss" : "ws";
		return URI.create(scheme + "://" + h + ":" + port + "/ws");
	}
}
