package dev.objz.commandbridge.config;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerSaveTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static BackendsConfig sample() {
        BackendsConfig d = BackendsConfig.defaults();
        return new BackendsConfig(
                "survival",
                EndpointType.WEBSOCKET,
                new BackendsConfig.Endpoints(
                        new BackendsConfig.Endpoints.WebSocket("play.example.net", 8765),
                        d.endpoints().redis()),
                new BackendsConfig.Security(TlsMode.TOFU, "", "s3cr3t"),
                d.timeouts(),
                false);
    }

    @Test
    void savedConfigLoadsBackEqual(@TempDir Path dir) {
        var manager = new ConfigManager<>(dir, BackendsConfig.class);
        manager.load();
        BackendsConfig cfg = sample();

        assertTrue(manager.save(cfg));
        assertEquals(cfg, manager.current());

        var other = new ConfigManager<>(dir, BackendsConfig.class);
        assertTrue(other.load());
        assertEquals(cfg, other.current());
    }

    @Test
    void saveReplacesFileAtomically(@TempDir Path dir) {
        var manager = new ConfigManager<>(dir, BackendsConfig.class);
        manager.load();

        assertTrue(manager.save(sample()));

        assertFalse(Files.exists(dir.resolve("config.yml.tmp")));
        assertTrue(Files.exists(dir.resolve("config.yml")));
    }
}
