package dev.objz.commandbridge.main.config;

import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.logging.Log;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ConfigManager {
	private final Path filePath;
	private volatile VelocityConfig config;

	public ConfigManager(Path dataDir) {
		this.filePath = dataDir.resolve("config.yml");
	}

	private YamlConfigurationLoader loader() {
		return YamlConfigurationLoader.builder()
				.path(filePath)
				.indent(2)
				.nodeStyle(NodeStyle.BLOCK)
				.build();
	}

	public VelocityConfig load() {
		try {
			Files.createDirectories(filePath.getParent());
		} catch (IOException e) {
			Log.error(e, "Failed to create config directory {}", filePath.getParent());
			return VelocityConfig.defaults();
		}

		if (!Files.exists(filePath)) {
			save(VelocityConfig.defaults());
		}

		try {
			var loader = loader();
			ConfigurationNode root = loader.load();

			Set<String> validKeys = Set.of(
					"bind-host", "bind-port", "server-id",
					"heartbeat", "security", "network",
					"limits", "debug");
			for (var key : root.childrenMap().keySet()) {
				String keyStr = String.valueOf(key);
				if (!validKeys.contains(keyStr)) {
					String suggestion = findClosest(keyStr, validKeys);
					if (suggestion != null) {
						Log.warn("Unknown config key: '{}' — did you mean '{}' ? (ignored)",
								keyStr, suggestion);
					} else {
						Log.warn("Unknown config key: '{}' (ignored)", keyStr);
					}
				}
			}

			VelocityConfig loaded = root.get(VelocityConfig.class, VelocityConfig.defaults());
			validate(loaded);
			this.config = loaded;
			return loaded;

		} catch (IllegalArgumentException e) {
			Log.error("Error loading config.yml: {}", e.getMessage());
			return VelocityConfig.defaults();
		} catch (Exception e) {
			Log.error("Error loading config.yml: " + e.getMessage(), e);
			return VelocityConfig.defaults();
		}
	}

	public void reload() {
		this.config = load();
	}

	public VelocityConfig current() {
		return config;
	}

	private void save(VelocityConfig defaults) {
		try {
			var loader = loader();
			ConfigurationNode root = loader.createNode();
			root.set(VelocityConfig.class, defaults);
			loader.save(root);
		} catch (Exception e) {
			Log.error(e, "Failed to save default config.yml: {}", e.getMessage());
		}
	}

	private static void validate(VelocityConfig cfg) {
		if (cfg.bindPort() <= 0 || cfg.bindPort() > 65535) {
			Log.error("bindPort must be between 1 and 65535");
		}
		if (cfg.limits().maxConnections() <= 0) {
			Log.error("maxConnections must be positive");
		}
		if (cfg.limits().maxMessageSizeBytes() < 1024) {
			Log.error("maxMessageSizeBytes too small");
		}
		if (cfg.heartbeat().appPingSeconds() <= 0) {
			Log.error("appPingSeconds must be > 0");
		}
		if (cfg.heartbeat().staleAfterSeconds() < cfg.heartbeat().appPingSeconds()) {
			Log.error("staleAfterSeconds must be >= appPingSeconds");
		}
	}

	private static String findClosest(String input, Set<String> options) {
		return options.stream()
				.min(Comparator.comparingInt(o -> levenshtein(o, input)))
				.filter(opt -> levenshtein(opt, input) <= 3)
				.orElse(null);
	}

	private static int levenshtein(String a, String b) {
		int[] costs = new int[b.length() + 1];
		for (int j = 0; j < costs.length; j++)
			costs[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			costs[0] = i;
			int nw = i - 1;
			for (int j = 1; j <= b.length(); j++) {
				int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
						a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
				nw = costs[j];
				costs[j] = cj;
			}
		}
		return costs[b.length()];
	}
}
