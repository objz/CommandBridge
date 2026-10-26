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
    void clientConfigSurvivesTheJsonItIsSealedAs() throws Exception {
        var c = dev.objz.commandbridge.config.model.BackendsConfig.defaults();
        assertEquals(c, MAPPER.readValue(MAPPER.writeValueAsBytes(c), dev.objz.commandbridge.config.model.BackendsConfig.class));
    }
}
