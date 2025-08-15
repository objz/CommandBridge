package dev.objz.commandbridge.main.config;

import dev.objz.commandbridge.main.config.profile.ConfigProfile;
import dev.objz.commandbridge.main.config.profile.BackendsConfigProfile;
import dev.objz.commandbridge.main.config.profile.VelocityConfigProfile;
import dev.objz.commandbridge.main.logging.Log;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public final class ConfigManager {
	private final Path filePath;
	private volatile Object current;

	// Register all supported config models here
	private static final Map<Class<?>, ConfigProfile<?>> PROFILES = Map.of(
			dev.objz.commandbridge.main.config.model.VelocityConfig.class, new VelocityConfigProfile(),
			dev.objz.commandbridge.main.config.model.BackendsConfig.class, new BackendsConfigProfile());

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

	public <T> T load(Class<T> modelClass) {
		ensureDir();

		if (!Files.exists(filePath)) {
			saveDefaults(modelClass);
		}

		try {
			var loader = loader();
			ConfigurationNode root = loader.load();

			ConfigProfile<T> profile = profileOf(modelClass);

			// unknown-key warnings
			Set<String> valid = profile.validKeys();
			for (var key : root.childrenMap().keySet()) {
				String k = String.valueOf(key);
				if (!valid.contains(k)) {
					String suggestion = findClosest(k, valid);
					if (suggestion != null) {
						Log.warn("Unknown config key: '{}' did you mean '{}' ? (ignored)", k,
								suggestion);
					} else {
						Log.warn("Unknown config key: '{}' (ignored)", k);
					}
				}
			}

			T loaded = root.get(modelClass, profile.defaults());
			profile.validate(loaded);
			this.current = loaded;
			return loaded;

		} catch (IllegalArgumentException e) {
			Log.error("Error loading config.yml: {}", e.getMessage());
			T d = profileOf(modelClass).defaults();
			this.current = d;
			return d;
		} catch (Exception e) {
			Log.error("Error loading config.yml: " + e.getMessage(), e);
			T d = profileOf(modelClass).defaults();
			this.current = d;
			return d;
		}
	}

	public <T> T reload(Class<T> modelClass) {
		return load(modelClass);
	}

	public <T> T current(Class<T> modelClass) {
		return modelClass.cast(current);
	}

	private void ensureDir() {
		try {
			Files.createDirectories(filePath.getParent());
		} catch (IOException e) {
			Log.error(e, "Failed to create config directory {}", filePath.getParent());
			throw new IllegalStateException("could not init config directory");
		}
	}

	private <T> void saveDefaults(Class<T> modelClass) {
		try {
			var loader = loader();
			ConfigurationNode root = loader.createNode();
			ConfigProfile<T> profile = profileOf(modelClass);
			root.set(modelClass, profile.defaults());
			loader.save(root);
		} catch (Exception e) {
			Log.error(e, "Failed to save default config.yml: {}", e.getMessage());
		}
	}

	private static <T> ConfigProfile<T> profileOf(Class<T> modelClass) {
		ConfigProfile<?> p = PROFILES.get(modelClass);
		if (p == null)
			throw new IllegalArgumentException("Unsupported config model: " + modelClass.getName());
		return (ConfigProfile<T>) p;
	}

	private static String findClosest(String input, Set<String> options) {
		return options.stream()
				.min(Comparator.comparingInt(o -> levenshtein(o, input)))
				.filter(opt -> levenshtein(opt, input) <= 3)
				.orElse(null);
	}

	private static int levenshtein(String a, String b) {
		int[] costs = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++)
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
