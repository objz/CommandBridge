package dev.objz.commandbridge.velocity.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.security.PairCrypto;
import dev.objz.commandbridge.velocity.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupSessionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    /** Records what the offer sends instead of talking to the relay. */
    static final class FakeSocket implements WebSocket {
        final List<String> sent = new ArrayList<>();
        boolean closed;

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            sent.add(data.toString());
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            closed = true;
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return closed;
        }

        @Override
        public boolean isInputClosed() {
            return closed;
        }

        @Override
        public void abort() {
            closed = true;
        }
    }

    @Test
    void pairOfferAnswersHelloWithSealedConfig() throws Exception {
        String code = PairCrypto.newCode();
        byte[] config = "{\"clientId\":\"creative\"}".getBytes(StandardCharsets.UTF_8);
        AtomicReference<JsonNode> paired = new AtomicReference<>();
        var offer = new PairOffer("creative", "main-proxy", code, config, paired::set);
        var socket = new FakeSocket();

        offer.onText(socket, "{\"type\":\"hello\",\"platform\":\"PAPER\",\"version\":\"1.21.4\"}", true);

        assertEquals(1, socket.sent.size());
        JsonNode reply = MAPPER.readTree(socket.sent.get(0));
        assertEquals("config", reply.path("type").asText());
        assertEquals("main-proxy", reply.path("proxy").asText());
        assertEquals(new String(config, StandardCharsets.UTF_8),
                new String(PairCrypto.open(code, reply.path("sealed").asText()), StandardCharsets.UTF_8));
        assertNull(paired.get());

        offer.onText(socket, "{\"type\":\"done\"}", true);

        assertEquals("PAPER", paired.get().path("platform").asText());
        assertEquals("1.21.4", paired.get().path("version").asText());
        assertTrue(socket.closed);
    }

    @Test
    void pairOfferAnswersOnlyOneHello() throws Exception {
        var offer = new PairOffer("creative", "main-proxy", PairCrypto.newCode(), new byte[]{'{', '}'}, n -> {
        });
        var socket = new FakeSocket();

        offer.onText(socket, "{\"type\":\"hello\",\"platform\":\"PAPER\",\"version\":\"1.21.4\"}", true);
        offer.onText(socket, "{\"type\":\"hello\",\"platform\":\"PAPER\",\"version\":\"1.21.4\"}", true);

        assertEquals(1, socket.sent.size());
    }

    @Test
    void aClosedOfferNeverHandsOutTheConfig() {
        var offer = new PairOffer("creative", "main-proxy", PairCrypto.newCode(), new byte[]{'{', '}'}, n -> {
        });
        var socket = new FakeSocket();
        offer.close();

        offer.onText(socket, "{\"type\":\"hello\"}", true);

        assertTrue(socket.sent.isEmpty());
    }

    @Test
    void repeatedDoneMessagesOnlyReportPairingOnce() {
        var count = new java.util.concurrent.atomic.AtomicInteger();
        var offer = new PairOffer("creative", "main-proxy", PairCrypto.newCode(), new byte[]{'{', '}'}, n -> count.incrementAndGet());
        var socket = new FakeSocket();
        offer.onText(socket, "{\"type\":\"hello\"}", true);

        offer.onText(socket, "{\"type\":\"done\"}", true);
        offer.onText(socket, "{\"type\":\"done\"}", true);

        assertEquals(1, count.get());
    }

    @Test
    void clientConfigSurvivesTheJsonItIsSealedAs() throws Exception {
        var c = dev.objz.commandbridge.config.model.BackendsConfig.defaults();
        assertEquals(c, MAPPER.readValue(MAPPER.writeValueAsBytes(c), dev.objz.commandbridge.config.model.BackendsConfig.class));
    }

    @Test
    void currentSettingsSurviveANormalizedConfig() {
        // normalize turns an empty keystore path into null
        var cfg = new dev.objz.commandbridge.config.profile.VelocityConfigProfile()
                .normalize(dev.objz.commandbridge.config.model.VelocityConfig.defaults()).config();

        var current = SetupSession.current(cfg);

        assertEquals("proxy-1", current.get("serverId"));
        assertEquals(java.util.Map.of("path", "", "type", "PKCS12"), current.get("keystore"));
    }

    @Test
    void firstUsePinIsTheProxysOwnCertificate(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        // the proxy keeps its TOFU keystore in data/, where TlsResolver creates it
        dev.objz.commandbridge.security.TLS.ensure(dir.resolve("data"), "localhost");
        var plan = new SetupPlan("main-proxy", dev.objz.commandbridge.config.model.EndpointType.WEBSOCKET, 8765,
                "play.example.net", null, dev.objz.commandbridge.config.model.TlsMode.TOFU, null, List.of());

        String pin = SetupSession.pin(plan, dir);

        assertTrue(pin.startsWith("sha256/"), pin);
        assertEquals(dev.objz.commandbridge.security.TLS.readServerSpkiPin(dir.resolve("data")), pin);
    }

    @Test
    void enablingTlsDuringSetupCreatesThePinBeforePairing(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        var plan = new SetupPlan("main-proxy", dev.objz.commandbridge.config.model.EndpointType.WEBSOCKET, 8765,
                "play.example.net", null, dev.objz.commandbridge.config.model.TlsMode.TOFU, null, List.of());

        String pin = SetupSession.pin(plan, dir);

        assertTrue(pin.startsWith("sha256/"), pin);
        assertEquals(dev.objz.commandbridge.security.TLS.readServerSpkiPin(dir.resolve("data")), pin);
    }

    @Test
    void aDefaultProxyWithoutSetupIsNotConfigured(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var manager = new dev.objz.commandbridge.config.ConfigManager<>(dir, dev.objz.commandbridge.config.model.VelocityConfig.class);
        manager.load();

        assertTrue(!SetupSession.isConfigured(manager.current(), dir));

        java.nio.file.Files.createDirectories(dir.resolve("data"));
        java.nio.file.Files.writeString(dir.resolve("data").resolve("setup.json"), "{}");
        assertTrue(SetupSession.isConfigured(manager.current(), dir));
    }

    @Test
    void aChangedConfigCountsAsConfigured(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        var d = dev.objz.commandbridge.config.model.VelocityConfig.defaults();
        var cfg = new dev.objz.commandbridge.config.model.VelocityConfig(d.actAsClient(), "main-proxy", d.endpointType(),
                d.endpoints(), d.security(), d.timeouts(), d.tasks(), d.debug());

        assertTrue(SetupSession.isConfigured(cfg, dir));
    }

    @Test
    void theConsoleSummaryNamesWhatChanges() {
        var plan = new SetupPlan("main", dev.objz.commandbridge.config.model.EndpointType.WEBSOCKET, 8765, "play.example.net", null,
                dev.objz.commandbridge.config.model.TlsMode.TOFU, null,
                List.of(new SetupPlan.Server("lobby", "127.0.0.1:30066", false), new SetupPlan.Server("creative", "10.0.0.13:25565", true)));

        assertEquals("proxy main, WebSocket at play.example.net:8765, encrypted on first use, servers lobby, creative (new, 10.0.0.13:25565)",
                SetupSession.summary(plan));
    }

    private static SetupSession offlineSession(java.nio.file.Path dir) throws Exception {
        var config = new dev.objz.commandbridge.config.ConfigManager<>(dir,
                dev.objz.commandbridge.config.model.VelocityConfig.class);
        assertTrue(config.load());
        var proxy = (com.velocitypowered.api.proxy.ProxyServer) java.lang.reflect.Proxy.newProxyInstance(
                SetupSessionTest.class.getClassLoader(), new Class<?>[]{com.velocitypowered.api.proxy.ProxyServer.class},
                (instance, method, args) -> {
                    if (method.getName().equals("getAllServers"))
                        return List.of();
                    throw new UnsupportedOperationException(method.getName());
                });
        var constructor = SetupSession.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        SetupSession session = (SetupSession) constructor.newInstance(proxy, config,
                new dev.objz.commandbridge.velocity.net.session.SessionHub(), dir, dir.resolve("velocity.toml"),
                (java.util.function.Supplier<CompletableFuture<String>>) () -> CompletableFuture.completedFuture(""));
        var relayType = dev.objz.commandbridge.velocity.editor.RelayChannel.class;
        var relayConstructor = relayType.getDeclaredConstructor(String.class, String.class,
                dev.objz.commandbridge.velocity.editor.RelayChannel.Handler.class, Runnable.class);
        relayConstructor.setAccessible(true);
        Object relay = relayConstructor.newInstance("Setup test", "setup/",
                (dev.objz.commandbridge.velocity.editor.RelayChannel.Handler) msg -> {
                }, (Runnable) () -> {
                });
        var ws = relayType.getDeclaredField("ws");
        ws.setAccessible(true);
        ws.set(relay, new FakeSocket());
        var channel = SetupSession.class.getDeclaredField("channel");
        channel.setAccessible(true);
        channel.set(session, relay);
        return session;
    }

    private static Object field(SetupSession session, String name) throws Exception {
        var field = SetupSession.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(session);
    }

    private static void plan(SetupSession session, String json) throws Exception {
        var method = SetupSession.class.getDeclaredMethod("plan", JsonNode.class);
        method.setAccessible(true);
        method.invoke(session, MAPPER.readTree(json));
    }

    private static String stage(SetupSession session) throws Exception {
        var method = SetupSession.class.getDeclaredMethod("stage");
        method.setAccessible(true);
        method.invoke(session);
        return (String) field(session, "staged");
    }

    private static final String PLAN = """
            {"serverId":"main-proxy","endpointType":"WEBSOCKET","port":8765,"address":"localhost",
             "tlsMode":"PLAIN","servers":[]}
            """;

    @Test
    void invalidPlansRevokeThePreviousStagedApproval(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        SetupSession session = offlineSession(dir);
        plan(session, PLAN);
        String code = stage(session);

        plan(session, "{}");

        assertNull(session.confirm(code));
    }

    @Test
    void aFailedApplyRemainsUnappliedAndCanBeRetried(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        SetupSession session = offlineSession(dir);
        plan(session, PLAN);
        String code = stage(session);
        java.nio.file.Files.delete(dir.resolve("config.yml"));
        java.nio.file.Files.createDirectory(dir.resolve("config.yml"));
        java.nio.file.Files.writeString(dir.resolve("config.yml/keep"), "keep");

        SetupSession.Result failed = session.confirm(code);

        assertTrue(!failed.errors().isEmpty());
        assertTrue(!failed.restartNeeded());
        assertEquals(false, field(session, "applied"));
        assertEquals(code, field(session, "staged"));
        assertTrue(!java.nio.file.Files.exists(dir.resolve("data/setup.json")));

        java.nio.file.Files.delete(dir.resolve("config.yml/keep"));
        java.nio.file.Files.delete(dir.resolve("config.yml"));
        assertTrue(session.confirm(code).errors().isEmpty());
        assertEquals(true, field(session, "applied"));
    }
}
