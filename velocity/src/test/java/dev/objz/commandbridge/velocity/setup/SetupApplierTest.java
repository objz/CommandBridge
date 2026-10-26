package dev.objz.commandbridge.velocity.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.velocity.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupApplierTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static SetupPlan plan(int port, String address) {
        return new SetupPlan("main-proxy", EndpointType.WEBSOCKET, port, address, null, TlsMode.TOFU, null,
                List.of(new SetupPlan.Server("lobby", "127.0.0.1:30066", false),
                        new SetupPlan.Server("creative", "10.0.0.13:25565", true)));
    }

    private static VelocityConfig current() {
        VelocityConfig d = VelocityConfig.defaults();
        return new VelocityConfig(d.actAsClient(), "main-proxy", d.endpointType(), d.endpoints(), d.security(),
                new VelocityConfig.Timeouts(9, 11), new VelocityConfig.Tasks(42), true);
    }

    @Test
    void proxyConfigKeepsUntouchedSettings(@TempDir Path dir) {
        var applier = new SetupApplier(dir, dir.resolve("velocity.toml"));
        VelocityConfig out = applier.proxyConfig(current(), plan(9000, "play.example.net"));

        assertEquals(9000, out.endpoints().webSocket().bindPort());
        assertEquals(new VelocityConfig.Timeouts(9, 11), out.timeouts());
        assertEquals(42, out.tasks().expireAfter());
        assertTrue(out.debug());
    }

    @Test
    void clientConfigCarriesTheAddressAndSecret(@TempDir Path dir) {
        var applier = new SetupApplier(dir, dir.resolve("velocity.toml"));
        SetupPlan plan = plan(8765, "play.example.net");
        BackendsConfig c = applier.clientConfig(plan, plan.servers().get(1), "s3cr3t", "sha256/abc");

        assertEquals("creative", c.clientId());
        assertEquals(EndpointType.WEBSOCKET, c.endpointType());
        assertEquals("play.example.net", c.endpoints().websocket().host());
        assertEquals(8765, c.endpoints().websocket().port());
        assertEquals(TlsMode.TOFU, c.security().tlsMode());
        assertEquals("sha256/abc", c.security().tlsPin());
        assertEquals("s3cr3t", c.security().secret());
        assertEquals(BackendsConfig.defaults().timeouts(), c.timeouts());
    }

    @Test
    void noRestartWhenOnlyServersChange(@TempDir Path dir) {
        var applier = new SetupApplier(dir, dir.resolve("velocity.toml"));
        assertFalse(applier.needsRestart(current(), plan(8765, "play.example.net")));
    }

    @Test
    void restartWhenPortChanges(@TempDir Path dir) {
        var applier = new SetupApplier(dir, dir.resolve("velocity.toml"));
        assertTrue(applier.needsRestart(current(), plan(9000, "play.example.net")));
    }

    @Test
    void applyWritesConfigTomlAndSetupJson(@TempDir Path dir) throws Exception {
        Path toml = dir.resolve("velocity.toml");
        try (InputStream in = getClass().getResourceAsStream("/setup/velocity.toml")) {
            Files.write(toml, in.readAllBytes());
        }
        Path data = dir.resolve("plugin");
        var applier = new SetupApplier(data, toml);

        assertEquals(List.of(), applier.apply(VelocityConfig.defaults(), plan(8765, "play.example.net")));

        var manager = new ConfigManager<>(data, VelocityConfig.class);
        assertTrue(manager.load());
        assertEquals("main-proxy", manager.current().serverId());
        assertTrue(Files.readString(toml).contains("creative = \"10.0.0.13:25565\""));
        assertFalse(Files.readString(toml).contains("lobby = \"127.0.0.1:30066\"\nlobby"));
        JsonNode saved = new ObjectMapper().readTree(data.resolve("data").resolve("setup.json").toFile());
        assertEquals("play.example.net", saved.path("address").asText());
        assertEquals("creative", saved.path("servers").get(1).path("id").asText());
    }

    @Test
    void blankPasswordsKeepTheProxysOwn() {
        VelocityConfig d = VelocityConfig.defaults();
        VelocityConfig current = new VelocityConfig(false, "main-proxy", EndpointType.REDIS,
                new VelocityConfig.Endpoints(d.endpoints().webSocket(),
                        new VelocityConfig.Endpoints.Redis("redis.local", 6379, "cb", "hunter2")),
                new VelocityConfig.Security(TlsMode.STRICT, "/ks.p12", "kspass", "PKCS12"),
                d.timeouts(), d.tasks(), false);
        SetupPlan plan = new SetupPlan("main-proxy", EndpointType.REDIS, 0, "", new SetupPlan.Redis("redis.local", 6379, "cb", null),
                TlsMode.STRICT, new SetupPlan.Keystore("/ks.p12", "PKCS12", null), List.of())
                .withCurrentPasswords(current);

        assertEquals("hunter2", plan.redis().password());
        assertEquals("kspass", plan.keystore().password());
        assertFalse(new SetupApplier(Path.of("."), Path.of("velocity.toml")).needsRestart(current, plan));
    }

    @Test
    void planRejectsZeroAddress() {
        assertTrue(plan(8765, "0.0.0.0").problems()
                .contains("Servers can't reach 0.0.0.0. Use the address they connect to."));
        assertEquals(List.of(), plan(8765, "play.example.net").problems());
    }

    @Test
    void planReadsFromJson() throws Exception {
        JsonNode json = new ObjectMapper().readTree("""
                {"serverId":"main-proxy","endpointType":"WEBSOCKET","port":8765,"address":"play.example.net",
                 "tlsMode":"TOFU","servers":[{"id":"creative","address":"10.0.0.13:25565","addToVelocity":true}]}""");
        SetupPlan p = SetupPlan.fromJson(json);

        assertEquals("main-proxy", p.serverId());
        assertEquals(8765, p.port());
        assertEquals(List.of(new SetupPlan.Server("creative", "10.0.0.13:25565", true)), p.servers());
        assertEquals(List.of(), p.problems());
    }

    private static SetupPlan withServers(String address, List<SetupPlan.Server> servers) {
        return new SetupPlan("main-proxy", EndpointType.WEBSOCKET, 8765, address, null, TlsMode.TOFU, null, servers);
    }

    @Test
    void serverNamesCanNotBreakVelocityToml() {
        var plan = withServers("play.example.net", List.of(new SetupPlan.Server("evil\ntry = []", "10.0.0.1:25565", true)));
        assertTrue(plan.problems().stream().anyMatch(p -> p.contains("letters, digits")), plan.problems().toString());
    }

    @Test
    void serverAddressesMustBeHostAndPort() {
        for (String bad : List.of("10.0.0.1", "10.0.0.1:0", "10.0.0.1:70000", "a b:25565", "\"x\":25565", "999.1.1.1:25565")) {
            var plan = withServers("play.example.net", List.of(new SetupPlan.Server("creative", bad, true)));
            assertFalse(plan.problems().isEmpty(), bad);
        }
        var ok = withServers("play.example.net", List.of(new SetupPlan.Server("creative", "10.0.0.1:25565", true),
                new SetupPlan.Server("lobby", "mc.example.net:30066", false)));
        assertEquals(List.of(), ok.problems());
    }

    @Test
    void theProxyAddressMustBeAHost() {
        for (String bad : List.of("ws://play.example.net", "play.example.net:8765", "play example", "999.1.1.1"))
            assertFalse(withServers(bad, List.of()).problems().isEmpty(), bad);
        assertEquals(List.of(), withServers("localhost", List.of()).problems());
        assertEquals(List.of(), withServers("::1", List.of()).problems());
    }

    @Test
    void atMostSixtyFourServers() {
        List<SetupPlan.Server> many = new java.util.ArrayList<>();
        for (int i = 0; i < 65; i++)
            many.add(new SetupPlan.Server("s" + i, "10.0.0.1:" + (20000 + i), false));
        assertFalse(withServers("play.example.net", many).problems().isEmpty());
    }
}
