package dev.objz.commandbridge.config;

import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(java.util.logging.Logger.getLogger("config-test"));
        } catch (IllegalStateException ignored) {
            // already installed by another test class
        }
    }

    @Test
    void loadCreatesDefaultWhenFileMissing(@TempDir Path tempDir) {
        var manager = new ConfigManager<>(tempDir, BackendsConfig.class);

        boolean result = manager.load();

        assertTrue(result, "load() should return true for freshly-created defaults");
        assertTrue(Files.exists(tempDir.resolve("config.yml")), "config.yml should be created");
        assertNotNull(manager.current(), "current() should not be null after load");
    }

    @Test
    void loadReturnsTrueWithValidYaml(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, validYaml("lobby-1"));

        var manager = new ConfigManager<>(tempDir, BackendsConfig.class);
        boolean result = manager.load();

        assertTrue(result, "load() should return true for valid YAML");
        assertNotNull(manager.current(), "current() should not be null");
        assertTrue("lobby-1".equals(manager.current().clientId()),
                "clientId should match written YAML value");
    }

    @Test
    void reloadPicksUpChanges(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, validYaml("server-a"));

        var manager = new ConfigManager<>(tempDir, BackendsConfig.class);
        assertTrue(manager.load(), "initial load should succeed");
        assertTrue("server-a".equals(manager.current().clientId()),
                "clientId should be server-a after initial load");

        Files.writeString(configFile, validYaml("server-b"));
        assertTrue(manager.reload(), "reload should succeed");
        assertTrue("server-b".equals(manager.current().clientId()),
                "clientId should be server-b after reload");
    }

    @Test
    void currentReturnsNullBeforeLoad(@TempDir Path tempDir) {
        var manager = new ConfigManager<>(tempDir, BackendsConfig.class);

        assertNull(manager.current(), "current() should be null before any load");
    }

    @Test
    void loadWithMissingRequiredFieldFallsBackToDefault(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, "client-id: partial-server\n");

        var manager = new ConfigManager<>(tempDir, BackendsConfig.class);
        manager.load();

        assertNotNull(manager.current(), "current() should not be null even with partial YAML");
        assertNotNull(manager.current().clientId(),
                "clientId should be populated from partial YAML or defaults");
    }

    private static String validYaml(String clientId) {
        return """
                client-id: "%s"
                endpoint-type: WEBSOCKET
                endpoints:
                  websocket:
                    host: "127.0.0.1"
                    port: 8765
                  redis:
                    host: "127.0.0.1"
                    port: 6379
                    username: ""
                    password: ""
                security:
                  tls-mode: TOFU
                  tls-pin: ""
                  secret: "my-real-secret"
                timeouts:
                  auth-timeout: 5
                  reconnect-timeout: 60
                  reconnect-interval: 5
                debug: false
                """.formatted(clientId);
    }
}
