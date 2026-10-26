package dev.objz.commandbridge.config;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigManagerLoadTest {

    @TempDir
    Path dir;

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private ConfigManager<VelocityConfig> velocity(String yaml) throws IOException {
        Files.writeString(dir.resolve("config.yml"), yaml);
        return new ConfigManager<>(dir, VelocityConfig.class);
    }

    private String defaultYaml() throws IOException {
        Path seed = dir.resolve("seed");
        assertTrue(new ConfigManager<>(seed, VelocityConfig.class).load());
        return Files.readString(seed.resolve("config.yml"));
    }

    private static void assertDefaultsLoaded(VelocityConfig cfg) {
        VelocityConfig d = VelocityConfig.defaults();
        assertEquals(d.serverId(), cfg.serverId());
        assertEquals(d.endpointType(), cfg.endpointType());
        assertEquals(d.endpoints(), cfg.endpoints());
        assertEquals(d.security().tlsMode(), cfg.security().tlsMode());
        assertEquals(d.timeouts(), cfg.timeouts());
        assertEquals(d.tasks(), cfg.tasks());
    }

    @Test
    void missingFileWritesDefaultsAndLoadsThem() {
        var manager = new ConfigManager<>(dir.resolve("nested"), VelocityConfig.class);

        assertTrue(manager.load());
        assertTrue(Files.exists(dir.resolve("nested").resolve("config.yml")));
        assertDefaultsLoaded(manager.current());
    }

    @Test
    void missingBackendsFileWritesDefaultsAndLoadsThem() {
        var manager = new ConfigManager<>(dir, "backend.yml", BackendsConfig.class);

        assertTrue(manager.load());
        assertTrue(Files.exists(dir.resolve("backend.yml")));
        assertEquals(BackendsConfig.defaults(), manager.current());
    }

    @Test
    void writtenDefaultsLoadAgainUnchanged() {
        var first = new ConfigManager<>(dir, VelocityConfig.class);
        assertTrue(first.load());

        var second = new ConfigManager<>(dir, VelocityConfig.class);
        assertTrue(second.load());
        assertEquals(first.current(), second.current());
    }

    @Test
    void enumValuesAreCaseInsensitive() throws IOException {
        String yaml = defaultYaml();
        assertTrue(yaml.contains("tls-mode: TOFU") && yaml.contains("endpoint-type: WEBSOCKET"), yaml);
        var manager = velocity(yaml.replace("tls-mode: TOFU", "tls-mode: tofu")
                .replace("endpoint-type: WEBSOCKET", "endpoint-type: redis"));

        assertTrue(manager.load());
        assertEquals(TlsMode.TOFU, manager.current().security().tlsMode());
        assertEquals(EndpointType.REDIS, manager.current().endpointType());
    }

    @Test
    void misspelledEnumFailsAndKeepsDefaults() throws IOException {
        var manager = velocity(defaultYaml().replace("server-id: proxy-1", "server-id: custom")
                .replace("tls-mode: TOFU", "tls-mode: tofuu"));

        assertFalse(manager.load());
        assertDefaultsLoaded(manager.current());
    }

    @Test
    void blankEnumValueFails() throws IOException {
        var manager = velocity(defaultYaml().replace("endpoint-type: WEBSOCKET", "endpoint-type: \"\""));

        assertFalse(manager.load());
        assertDefaultsLoaded(manager.current());
    }

    @Test
    void unknownTopLevelKeyIsIgnored() throws IOException {
        var manager = velocity(defaultYaml() + "server-idd: typo\nsomething-else: 1\n");

        assertTrue(manager.load());
        assertDefaultsLoaded(manager.current());
    }

    @Test
    void partialFileIsFilledFromDefaults() throws IOException {
        var manager = velocity("""
                server-id: lobby-proxy
                endpoints:
                  websocket:
                    bind-host: 10.0.0.1
                """);

        assertTrue(manager.load());
        VelocityConfig cfg = manager.current();
        VelocityConfig d = VelocityConfig.defaults();
        assertEquals("lobby-proxy", cfg.serverId());
        assertEquals("10.0.0.1", cfg.endpoints().webSocket().bindHost());
        assertEquals(d.endpoints().webSocket().bindPort(), cfg.endpoints().webSocket().bindPort());
        assertEquals(d.endpoints().redis(), cfg.endpoints().redis());
        assertEquals(d.security(), cfg.security());
        assertEquals(d.timeouts(), cfg.timeouts());
        assertEquals(d.tasks(), cfg.tasks());
        assertEquals(d.endpointType(), cfg.endpointType());
    }

    @Test
    void missingSectionKeepsItsDefaults() throws IOException {
        String yaml = defaultYaml();
        int start = yaml.indexOf("tasks:");
        int end = yaml.indexOf("\n", yaml.indexOf("expire-after:", start)) + 1;
        var manager = velocity(yaml.substring(0, start) + yaml.substring(end));

        assertTrue(manager.load());
        assertEquals(86400, manager.current().tasks().expireAfter());
    }

    @Test
    void invalidValueIsNormalizedButReported() throws IOException {
        var manager = velocity(defaultYaml().replace("bind-port: 8765", "bind-port: 70000"));

        assertFalse(manager.load());
        assertEquals(8765, manager.current().endpoints().webSocket().bindPort());
    }

    @Test
    void malformedYamlFailsAndKeepsDefaults() throws IOException {
        var manager = velocity("""
                server-id: [unclosed
                security:
                  tls-mode: TOFU
                """);

        assertFalse(manager.load());
        assertDefaultsLoaded(manager.current());
    }

    @Test
    void wrongValueTypeFails() throws IOException {
        var manager = velocity("""
                timeouts:
                  register-timeout: soon
                """);

        assertFalse(manager.load());
        assertDefaultsLoaded(manager.current());
    }
}
