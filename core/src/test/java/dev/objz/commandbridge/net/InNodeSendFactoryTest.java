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

final class InNodeSendFactoryTest {
    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void registeredHandlersReceiveLateAndReplacementFactories() throws Exception {
        List<String> factories = new ArrayList<>();
        List<Envelope> replies = new ArrayList<>();
        Endpoint endpoint = new Endpoint() {
            public CompletableFuture<Void> send(Envelope env) {
                replies.add(env);
                return CompletableFuture.completedFuture(null);
            }

            public boolean isOpen() {
                return true;
            }
        };
        InNode node = new InNode().register(MessageType.PING, new InboundHandler() {
            public void accept(Endpoint ep, Envelope env) {
                reply(ep, env, MessageType.PONG, null).dispatch().join();
            }
        });
        String request = Envelope.MAPPER.writeValueAsString(
                Envelope.make(MessageType.PING, "proxy", "backend", null));

        for (String factory : List.of("initial", "replacement")) {
            node.setSendOperationFactory((ep, env) -> {
                factories.add(factory);
                return new SendOperation(ep, env, new ResponseAwaiter());
            });
            node.onText(endpoint, request);
        }

        assertEquals(List.of("initial", "replacement"), factories);
        assertEquals(2, replies.size());
        assertEquals(MessageType.PONG, replies.getFirst().type());
    }
}
