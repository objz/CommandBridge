package dev.objz.commandbridge.main.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.net.URI;

@ConfigSerializable
public record BackendsConfig(
		@Setting("host") String host, // hostname or IP only (no scheme)
		@Setting("port") int port,
		@Setting("tls") boolean tls, // true = wss, false = ws
		@Setting("client-id") String clientId,
		@Setting("secret") String secret,
		@Setting("debug") boolean debug) {

	public static BackendsConfig defaults() {
		return new BackendsConfig(
				"127.0.0.1",
				8765,
				true, // default to wss
				"survival-1",
				"change-me",
				false);
	}

	public URI uri() {
		String h = host != null ? host.trim() : "";
		if (h.isEmpty())
			throw new IllegalStateException("host must not be empty");

		// Backward compatibility: if users still provide ws:// or wss:// in host, honor it 69
		if (hasScheme(h)) {
			return URI.create(h + ":" + port + "/ws");
		}

		String scheme = tls ? "wss" : "ws";
		String authority = needsIpv6Brackets(h) ? "[" + h + "]" : h;
		return URI.create(scheme + "://" + authority + ":" + port + "/ws");
	}

	private static boolean hasScheme(String h) {
		return h.startsWith("ws://") || h.startsWith("wss://");
	}

	private static boolean needsIpv6Brackets(String h) {
		// If it's an IPv6 literal without brackets
		return h.indexOf(':') >= 0 && !(h.startsWith("[") && h.endsWith("]"));
	}
}
