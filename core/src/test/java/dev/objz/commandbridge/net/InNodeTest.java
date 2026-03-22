package dev.objz.commandbridge.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InNodeTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
            // expected
        }
    }

    private static final class CountingHandler extends InboundHandler {
        final AtomicInteger count = new AtomicInteger(0);
        Envelope lastEnv;

        @Override
        public void accept(Endpoint endpoint, Envelope env) {
            count.incrementAndGet();
            this.lastEnv = env;
        }
    }

    @Test
    void registersAndDispatchesByType() throws Exception {
        InNode inNode = new InNode();
        CountingHandler handler = new CountingHandler();
        inNode.register(MessageType.PING, handler);

        Envelope testEnv = Envelope.make(MessageType.PING, "server1", "client1", null);
        String json = Envelope.MAPPER.writeValueAsString(testEnv);

        TestEndpoint endpoint = new TestEndpoint();
        inNode.onText(endpoint, json);

        assertEquals(1, handler.count.get());
        assertNotNull(handler.lastEnv);
        assertEquals(MessageType.PING, handler.lastEnv.type());
    }

    @Test
    void unhandledTypeDoesNotThrow() throws Exception {
        InNode inNode = new InNode();
        CountingHandler handler = new CountingHandler();
        inNode.register(MessageType.PING, handler);

        Envelope testEnv = Envelope.make(MessageType.PONG, "server1", "client1", null);
        String json = Envelope.MAPPER.writeValueAsString(testEnv);

        TestEndpoint endpoint = new TestEndpoint();
        assertDoesNotThrow(() -> inNode.onText(endpoint, json));
        assertEquals(0, handler.count.get());
    }

    @Test
    void badJsonDoesNotThrow() {
        InNode inNode = new InNode();
        TestEndpoint endpoint = new TestEndpoint();

        assertDoesNotThrow(() -> inNode.onText(endpoint, "not valid json"));
    }

    @Test
    void tapInterceptsBeforeHandler() throws Exception {
        InNode inNode = new InNode();
        CountingHandler handler = new CountingHandler();
        inNode.register(MessageType.PING, handler);
        inNode.setInboundTap(env -> true);

        Envelope testEnv = Envelope.make(MessageType.PING, "server1", "client1", null);
        String json = Envelope.MAPPER.writeValueAsString(testEnv);

        TestEndpoint endpoint = new TestEndpoint();
        inNode.onText(endpoint, json);

        assertEquals(0, handler.count.get());
    }

    @Test
    void tapFalsePassesToHandler() throws Exception {
        InNode inNode = new InNode();
        CountingHandler handler = new CountingHandler();
        inNode.register(MessageType.PING, handler);
        inNode.setInboundTap(env -> false);

        Envelope testEnv = Envelope.make(MessageType.PING, "server1", "client1", null);
        String json = Envelope.MAPPER.writeValueAsString(testEnv);

        TestEndpoint endpoint = new TestEndpoint();
        inNode.onText(endpoint, json);

        assertEquals(1, handler.count.get());
    }

    @Test
    void nullEndpointHandledGracefully() {
        InNode inNode = new InNode();

        assertDoesNotThrow(() -> inNode.onText(null, "bad json"));
    }
}
