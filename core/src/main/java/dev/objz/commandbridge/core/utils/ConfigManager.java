package dev.objz.commandbridge.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import dev.objz.commandbridge.core.Logger;

public class ConfigManager {

    private final Map<String, Map<String, Object>> configData = new HashMap<>();
    private String secret;
    private final Yaml yaml;
    private final Logger logger;
    private final String configDirectory;
    private final String secretFileName;

    public ConfigManager(Logger logger, String pluginName) {
        this(logger, pluginName, "secret.key");
    }

    public ConfigManager(Logger logger, String pluginName, String secretFileName) {
        this.logger = logger;
        this.configDirectory = "plugins" + File.separator + (pluginName != null ? pluginName : "FoundationLib");
        this.secretFileName = secretFileName;

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    public void loadAllConfigs() {
        File configDir = new File(configDirectory);
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw new RuntimeException("Failed to create config directory: " + configDirectory);
        }

        try {
            Files.list(configDir.toPath())
                    .filter(path -> path.toString().endsWith(".yml"))
                    .forEach(this::loadConfigFile);

            logger.debug("All configuration files have been loaded from directory: {}", configDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration files: " + e.getMessage(), e);
        }
    }

    public void reload() {
        configData.clear();

        loadAllConfigs();

        logger.info("All configurations have been successfully reloaded");
    }

    private void loadConfigFile(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            Map<String, Object> fileconfigData = yaml.load(inputStream);
            if (fileconfigData == null) {
                fileconfigData = new HashMap<>();
            }
            configData.put(path.getFileName().toString(), fileconfigData);
            logger.debug("Config file loaded successfully: {}", path.getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + e.getMessage(), e);
        }
    }

    public void loadSecret() {
        File secretFile = new File(configDirectory, secretFileName);

        if (!secretFile.exists()) {
            generateSecret();
        }
        try (InputStream inputStream = Files.newInputStream(secretFile.toPath())) {
            secret = new String(inputStream.readAllBytes());
            logger.debug("Secret file loaded successfully from path: {}", secretFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load secret file: " + e.getMessage(), e);
        }
    }

    public String getKey(String fileName, String key) {
        Map<String, Object> fileConfigData = configData.get(fileName);
        if (fileConfigData != null && fileConfigData.containsKey(key)) {
            logger.debug("Retrieved key '{}' from config: {}", key, fileName);
            return fileConfigData.get(key).toString();
        } else {
            throw new RuntimeException("Key '" + key + "' not found in config: " + fileName);
        }
    }

    public String getSecret() {
        if (secret != null) {
            logger.debug("Retrieved secret");
            return secret;
        } else {
            logger.error("Secret not found");
            return null;
        }
    }

    protected void generateSecret() {
        File configDir = new File(configDirectory);
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw new RuntimeException("Failed to create config directory: " + configDirectory);
        }

        File secretFile = new File(configDir, secretFileName);

        if (secretFile.exists()) {
            logger.debug("Secret file already exists, skipping copy");
            return;
        }

        try (OutputStream out = Files.newOutputStream(secretFile.toPath())) {
            String secret = TLSUtils.generateSecret();
            out.write(secret.getBytes());
            logger.info("Secret file generated successfully at: {}", secretFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate secret file: " + e.getMessage(), e);
        }
    }

    public void copyConfig(String resourceName, String targetFileName) {
        File configDir = new File(configDirectory);
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw new RuntimeException("Failed to create config directory: " + configDirectory);
        }

        File configFile = new File(configDir, targetFileName);

        if (configFile.exists()) {
            logger.debug("Config file '{}' already exists, skipping copy", configFile.getAbsolutePath());
            return;
        }

        try (InputStream in = getClass().getResourceAsStream("/" + resourceName);
                OutputStream out = Files.newOutputStream(configFile.toPath())) {
            if (in == null) {
                throw new RuntimeException("Resource '" + resourceName + "' not found in the plugin JAR");
            }

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            logger.info("Default config '{}' copied to: {}", resourceName, configFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy default config file " + resourceName + ": " + e.getMessage(), e);
        }
    }
}
