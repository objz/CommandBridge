package dev.objz.commandbridge.net;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InNodeGateTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static final Endpoint STRANGER = new Endpoint() {
        @Override
        public CompletableFuture<Void> send(Envelope env) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isOpen() {
            return true;
        }
    };

    private static String text(MessageType type) throws Exception {
        return Envelope.MAPPER.writeValueAsString(Envelope.make(type, "lobby", "proxy", null));
    }

    @Test
    void aClosedGateDropsTheMessageBeforeTheTapAndTheHandler() throws Exception {
        List<MessageType> handled = new ArrayList<>();
        List<MessageType> tapped = new ArrayList<>();
        InNode node = new InNode();
        node.register(MessageType.PLUGIN_MESSAGE, new InboundHandler() {
            @Override
            public void accept(Endpoint endpoint, Envelope env) {
                handled.add(env.type());
            }
        });
        node.setInboundTap(env -> {
            tapped.add(env.type());
            return false;
        });
        node.setGate((endpoint, env) -> env.type() == MessageType.AUTH_REQUEST);

        node.onText(STRANGER, text(MessageType.PLUGIN_MESSAGE));

        assertTrue(handled.isEmpty());
        assertTrue(tapped.isEmpty());
    }

    @Test
    void anOpenGateLetsTheMessageThrough() throws Exception {
        List<MessageType> handled = new ArrayList<>();
        InNode node = new InNode();
        node.register(MessageType.AUTH_REQUEST, new InboundHandler() {
            @Override
            public void accept(Endpoint endpoint, Envelope env) {
                handled.add(env.type());
            }
        });
        node.setGate((endpoint, env) -> env.type() == MessageType.AUTH_REQUEST);

        node.onText(STRANGER, text(MessageType.AUTH_REQUEST));

        assertEquals(List.of(MessageType.AUTH_REQUEST), handled);
    }

    @Test
    void nullAndIncompleteEnvelopesAreRejectedBeforeTheGateOrTap() {
        InNode node = new InNode();
        node.setGate((endpoint, env) -> {
            throw new AssertionError("Malformed envelope reached the gate");
        });
        node.setInboundTap(env -> {
            throw new AssertionError("Malformed envelope reached the tap");
        });

        for (String json : List.of("null", "{}", "{\"type\":null}"))
            node.onText(STRANGER, json);
    }
}
