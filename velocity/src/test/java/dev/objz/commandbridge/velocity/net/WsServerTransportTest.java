package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import io.undertow.Undertow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Exercises the Undertow WebSocket handshake and text framing on a loopback port. */
final class WsServerTransportTest {
    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    @Timeout(15)
    void receivesAnEnvelopeOverARealWebSocket() throws Exception {
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
        WebSocket socket = null;
        HttpClient http = HttpClient.newHttpClient();
        try {
            Field field = WsServer.class.getDeclaredField("server");
            field.setAccessible(true);
            Undertow undertow = (Undertow) field.get(server);
            int port = ((InetSocketAddress) undertow.getListenerInfo().getFirst().getAddress()).getPort();
            socket = http.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws"),
                    new WebSocket.Listener() {
                    }).get(5, TimeUnit.SECONDS);
            Envelope expected = Envelope.make(MessageType.PING, "lobby", "proxy", null);
            socket.sendText(Envelope.MAPPER.writeValueAsString(expected), true).get(5, TimeUnit.SECONDS);
            assertEquals(expected, received.get(5, TimeUnit.SECONDS));
        } finally {
            if (socket != null) {
                socket.abort();
            }
            http.close();
            server.stop();
        }
    }
}
