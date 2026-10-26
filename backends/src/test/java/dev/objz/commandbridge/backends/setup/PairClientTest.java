package dev.objz.commandbridge.backends.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.security.PairCrypto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PairClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static BackendsConfig paired() {
        BackendsConfig d = BackendsConfig.defaults();
        return new BackendsConfig("survival", EndpointType.WEBSOCKET,
                new BackendsConfig.Endpoints(new BackendsConfig.Endpoints.WebSocket("play.example.net", 8765),
                        d.endpoints().redis()),
                new BackendsConfig.Security(TlsMode.TOFU, "", "s3cr3t"), d.timeouts(), false);
    }

    /** Plays the proxy's side of the pair channel: answers hello with the sealed config. */
    static final class FakeProxy implements WebSocket {
        final String code;
        final List<JsonNode> received = new ArrayList<>();
        URI uri;
        Listener listener;

        FakeProxy(String code) {
            this.code = code;
        }

        CompletableFuture<WebSocket> connect(URI uri, Listener listener) {
            this.uri = uri;
            this.listener = listener;
            listener.onOpen(this);
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            try {
                JsonNode msg = MAPPER.readTree(data.toString());
                received.add(msg);
                if (msg.path("type").asText().equals("hello")) {
                    String sealed = PairCrypto.seal(code, MAPPER.writeValueAsBytes(paired()));
                    String reply = MAPPER.writeValueAsString(Map.of("type", "config", "proxy", "main-proxy", "sealed", sealed));
                    CompletableFuture.runAsync(() -> listener.onText(this, reply, true));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
            return false;
        }

        @Override
        public boolean isInputClosed() {
            return false;
        }

        @Override
        public void abort() {
        }
    }

    @Test
    void pairWritesTheSealedConfigAndKeepsABackup(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), "client-id: old\n");
        String code = PairCrypto.newCode();
        FakeProxy proxy = new FakeProxy(code);
        var client = new PairClient(dir, "ws://relay.test/", proxy::connect);

        PairClient.Paired result = client.pair(code.toLowerCase().replace("-", " "), "PAPER", "1.21.4")
                .get(5, TimeUnit.SECONDS);

        assertEquals("main-proxy", result.proxyId());
        assertEquals(URI.create("ws://relay.test/api/pair/" + PairCrypto.channelId(code) + "/ws?role=server"), proxy.uri);
        assertEquals("hello", proxy.received.get(0).path("type").asText());
        assertEquals("PAPER", proxy.received.get(0).path("platform").asText());
        assertEquals("done", proxy.received.get(proxy.received.size() - 1).path("type").asText());
        assertEquals("client-id: old\n", Files.readString(dir.resolve("config.yml.bak")));
        var manager = new ConfigManager<>(dir, BackendsConfig.class);
        assertTrue(manager.load());
        assertEquals("survival", manager.current().clientId());
        assertEquals("s3cr3t", manager.current().security().secret());
    }

    @Test
    void wrongCodeExplains(@TempDir Path dir) {
        var client = new PairClient(dir, "ws://relay.test/",
                (uri, listener) -> CompletableFuture.failedFuture(new WebSocketHandshakeException(new Status(404))));

        var e = assertThrows(ExecutionException.class,
                () -> client.pair("7KQM-4XR2-9W", "PAPER", "1.21.4").get(5, TimeUnit.SECONDS));
        assertEquals("No setup is waiting for this code. Check it, or run /cb setup again on the proxy",
                e.getCause().getMessage());
        assertFalse(Files.exists(dir.resolve("config.yml.bak")));
    }

    @Test
    void secretIsNotLogged(@TempDir Path dir) throws Exception {
        List<String> lines = new ArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                lines.add(String.valueOf(record.getMessage()));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger.getLogger("test").addHandler(capture);
        try {
            String code = PairCrypto.newCode();
            FakeProxy proxy = new FakeProxy(code);
            new PairClient(dir, "ws://relay.test/", proxy::connect).pair(code, "PAPER", "1.21.4").get(5, TimeUnit.SECONDS);
        } finally {
            Logger.getLogger("test").removeHandler(capture);
        }
        assertTrue(lines.stream().noneMatch(l -> l.contains("s3cr3t")), String.join("\n", lines));
    }

    /** The relay's refusal as java.net.http reports it. */
    record Status(int statusCode) implements HttpResponse<String> {
        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public String body() {
            return "";
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    @Test
    void aFreshConfigIsUnpaired(@TempDir Path dir) {
        var manager = new ConfigManager<>(dir, BackendsConfig.class);
        manager.load();

        assertTrue(PairClient.isUnpaired(manager.current()));
        assertFalse(PairClient.isUnpaired(paired()));
    }
}
