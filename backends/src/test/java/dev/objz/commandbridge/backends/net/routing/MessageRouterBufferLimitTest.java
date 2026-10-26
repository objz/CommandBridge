package dev.objz.commandbridge.backends.net.routing;

import dev.objz.commandbridge.api.platform.ConnectionState;
import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.scripting.model.enums.Location;
import io.undertow.Handlers;
import io.undertow.Undertow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageRouterBufferLimitTest {
    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oversizedMessagesAreClosedBeforeAuthentication(boolean binary) throws Exception {
        CompletableFuture<Void> reconnect = new CompletableFuture<>();
        MessageRouter router = new MessageRouter(new InNode(), new OutNode(), new ResponseAwaiter(),
                new AtomicReference<>(ConnectionState.CONNECTED), () -> {
                    reconnect.complete(null);
                }, "test-secret", Location.BACKEND);
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1")
                .setHandler(Handlers.websocket((exchange, channel) -> router.setupChannel(channel))).build();
        server.start();
        try (HttpClient client = HttpClient.newHttpClient()) {
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            CompletableFuture<Integer> closed = new CompletableFuture<>();
            WebSocket socket = client.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:" + port + "/"),
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
                reconnect.get(5, TimeUnit.SECONDS);
            } finally {
                socket.abort();
            }
        } finally {
            server.stop();
        }
    }
}
