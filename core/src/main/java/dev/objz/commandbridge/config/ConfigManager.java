package dev.objz.commandbridge.config;

import dev.objz.commandbridge.config.profile.ConfigProfile;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.config.profile.BackendsConfigProfile;
import dev.objz.commandbridge.config.profile.VelocityConfigProfile;
import dev.objz.commandbridge.logging.Log;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ConfigManager {
    private final Path filePath;
    private volatile Object current;

    private static final Map<Class<?>, ConfigProfile<?>> PROFILES = Map.of(VelocityConfig.class,
            new VelocityConfigProfile(), BackendsConfig.class, new BackendsConfigProfile());

    public ConfigManager(Path dataDir) {
        this.filePath = dataDir.resolve("config.yml");
    }

    public ConfigManager(Path dataDir, String name) {
        this.filePath = dataDir.resolve(name);
    }

    private YamlConfigurationLoader loader() {
        return YamlConfigurationLoader.builder()
                .path(filePath)
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
    }

    public <T> boolean load(Class<T> modelClass) {
        ensureDir();

        if (!Files.exists(filePath)) {
            saveDefaults(modelClass);
        }

        try {
            var loader = loader();
            ConfigurationNode root = loader.load();

            ConfigProfile<T> profile = profileOf(modelClass);
            T defaults = profile.defaults();

            Set<String> valid = new HashSet<>(ConfigKeys.topLevelKeysOf(modelClass));
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

            boolean enumOk = validateEnumValues(root, modelClass, "");
            if (!enumOk) {
                this.current = defaults;
                Log.error("Invalid config.yml");
                return false;
            }

            T loaded = root.get(modelClass, defaults);

            var result = profile.normalize(loaded);
            this.current = result.config();

            if (!result.ok()) {
                Log.error("Invalid config.yml");
            }
            return result.ok();

        } catch (SerializationException e) {
            Log.error("Invalid config.yml: {}", e.getMessage());
            this.current = profileOf(modelClass).defaults();
            return false;
        } catch (Exception e) {
            Log.error("Error loading config.yml: " + e.getMessage(), e);
            this.current = profileOf(modelClass).defaults();
            return false;
        }
    }

    public <T> boolean reload(Class<T> modelClass) {
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

    private static boolean validateEnumValues(ConfigurationNode node, Class<?> recordType, String path) {
        if (!recordType.isRecord() || node == null) {
            return true;
        }

        boolean ok = true;
        for (RecordComponent rc : recordType.getRecordComponents()) {
            String key = yamlKeyFor(recordType, rc);
            Class<?> componentType = rc.getType();
            ConfigurationNode child = node.node(key);
            String fullPath = path == null || path.isBlank() ? key : path + "." + key;

            if (componentType.isEnum()) {
                if (child.virtual()) {
                    continue;
                }

                Object raw = child.raw();
                String rawText = raw == null ? null : String.valueOf(raw).trim();
                Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) componentType;

                if (rawText == null || rawText.isBlank()) {
                    Log.error("'{}' must be set", fullPath);
                    ok = false;
                    continue;
                }

                String canonical = canonicalEnumValue(enumType, rawText);
                if (canonical != null) {
                    if (!canonical.equals(rawText)) {
                        try {
                            child.set(canonical);
                        } catch (SerializationException ignored) {
                        }
                    }
                    continue;
                }

                Set<String> values = enumConstants(enumType);
                String suggestion = findClosest(rawText.toUpperCase(Locale.ROOT), values);
                if (suggestion != null) {
                    Log.warn("Invalid value '{}' for '{}'. did you mean '{}' ?", rawText, fullPath, suggestion);
                } else {
                    Log.error("Invalid value '{}' for '{}'. expected one of {}", rawText, fullPath, values);
                }
                ok = false;
            } else if (componentType.isRecord()) {
                ok &= validateEnumValues(child, componentType, fullPath);
            }
        }

        return ok;
    }

    private static String yamlKeyFor(Class<?> recordType, RecordComponent rc) {
        try {
            Method m = recordType.getMethod(rc.getName());
            Setting s = m.getAnnotation(Setting.class);
            if (s != null && !s.value().isBlank()) {
                return s.value();
            }
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Field f = recordType.getDeclaredField(rc.getName());
            Setting s = f.getAnnotation(Setting.class);
            if (s != null && !s.value().isBlank()) {
                return s.value();
            }
        } catch (NoSuchFieldException ignored) {
        }

        Setting s = rc.getAnnotation(Setting.class);
        if (s != null && !s.value().isBlank()) {
            return s.value();
        }

        return rc.getName().replaceAll("(?<!^)([A-Z])", "-$1").toLowerCase(Locale.ROOT);
    }

    private static Set<String> enumConstants(Class<? extends Enum<?>> enumType) {
        Set<String> values = new HashSet<>();
        for (Enum<?> constant : enumType.getEnumConstants()) {
            values.add(constant.name());
        }
        return values;
    }

    private static String canonicalEnumValue(Class<? extends Enum<?>> enumType, String raw) {
        for (Enum<?> constant : enumType.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(raw)) {
                return constant.name();
            }
        }
        return null;
    }
}
