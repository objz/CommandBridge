package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import io.undertow.Undertow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WsServerBufferLimitTest {
    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static int port(WsServer server) throws Exception {
        var field = WsServer.class.getDeclaredField("server");
        field.setAccessible(true);
        Undertow undertow = (Undertow) field.get(server);
        return ((InetSocketAddress) undertow.getListenerInfo().getFirst().getAddress()).getPort();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oversizedMessagesAreClosedBeforeAuthentication(boolean binary) throws Exception {
        WsServer server = new WsServer("127.0.0.1", 0, new SessionHub(), new InNode());
        server.start();
        try (HttpClient client = HttpClient.newHttpClient()) {
            int port = port(server);
            CompletableFuture<Integer> closed = new CompletableFuture<>();
            WebSocket socket = client.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws"),
                    new WebSocket.Listener() {
                        @Override
                        public java.util.concurrent.CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
                            closed.complete(status);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket ws, Throwable error) {
                            closed.completeExceptionally(error);
                        }
                    }).get(5, TimeUnit.SECONDS);
            try {
                int chunk = 512 * 1024;
                for (int i = 0; i < 9; i++) {
                    if (binary)
                        socket.sendBinary(ByteBuffer.allocate(chunk), i == 8).get(5, TimeUnit.SECONDS);
                    else
                        socket.sendText("x".repeat(chunk), i == 8).get(5, TimeUnit.SECONDS);
                }
                assertEquals(1009, closed.get(5, TimeUnit.SECONDS));
            } finally {
                socket.abort();
            }
        } finally {
            server.stop();
        }
    }

    @Test
    void validFragmentedUnicodeStillReachesTheRouter() throws Exception {
        CompletableFuture<Envelope> received = new CompletableFuture<>();
        InNode router = new InNode();
        WsServer server = new WsServer("127.0.0.1", 0, new SessionHub(), router);
        router.register(MessageType.PING, new InboundHandler() {
            @Override
            public void accept(Endpoint endpoint, Envelope env) {
                received.complete(env);
            }
        });
        server.start();
        try (HttpClient client = HttpClient.newHttpClient()) {
            WebSocket socket = client.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:" + port(server) + "/ws"),
                    new WebSocket.Listener() {
                    }).get(5, TimeUnit.SECONDS);
            try {
                Envelope expected = Envelope.make(MessageType.PING, "lobby", "proxy", Envelope.MAPPER.valueToTree("😀é".repeat(5000)));
                String json = Envelope.MAPPER.writeValueAsString(expected);
                socket.sendText(json.substring(0, 50), false).get(5, TimeUnit.SECONDS);
                socket.sendText(json.substring(50), true).get(5, TimeUnit.SECONDS);
                assertEquals(expected, received.get(5, TimeUnit.SECONDS));
            } finally {
                socket.abort();
            }
        } finally {
            server.stop();
        }
    }
}
