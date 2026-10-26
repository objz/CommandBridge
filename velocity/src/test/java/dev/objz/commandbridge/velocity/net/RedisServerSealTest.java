package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.endpoints.RedisEndpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.net.redis.RedisChannels;
import dev.objz.commandbridge.security.RedisSeal;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests authentication before Redis messages reach the proxy router. */
final class RedisServerSealTest {
    private static Method handleInbound;
    private final RedisSeal signer = new RedisSeal("test-secret");
    private final List<String> receivedFrom = new ArrayList<>();
    private RedisServer server;

    @BeforeAll
    static void setup() throws Exception {
        TestFixtures.ensureLog();
        handleInbound = RedisServer.class.getDeclaredMethod("handleInbound", String.class);
        handleInbound.setAccessible(true);
    }

    @BeforeEach
    void createServer() {
        InNode router = new InNode();
        server = new RedisServer("localhost", 6379, "", "", new SessionHub(), router, "test-secret");
        router.register(MessageType.PING, new InboundHandler() {
            @Override
            public void accept(Endpoint endpoint, Envelope env) {
                receivedFrom.add(((RedisEndpoint) endpoint).id());
            }
        });
    }

    @Test
    void signedMessagesReachTheRouterOnlyOnce() throws Exception {
        String message = ping();
        String sealed = signer.seal(RedisChannels.PROXY_INBOUND, message);
        handleInbound.invoke(server, sealed);
        handleInbound.invoke(server, sealed);
        assertEquals(List.of("lobby"), receivedFrom);
    }

    @Test
    void unsignedMisroutedAndTamperedMessagesNeverReachTheRouter() throws Exception {
        String message = ping();
        handleInbound.invoke(server, message);
        handleInbound.invoke(server, new RedisSeal("other-secret").seal(RedisChannels.PROXY_INBOUND, message));
        handleInbound.invoke(server, signer.seal(RedisChannels.clientInbound("lobby"), message));
        handleInbound.invoke(server, signer.seal(RedisChannels.PROXY_INBOUND, message).replace("lobby", "survival"));
        assertTrue(receivedFrom.isEmpty());
    }

    @Test
    void signedMessageWithoutSenderNeverReachesTheRouter() throws Exception {
        handleInbound.invoke(server, signer.seal(RedisChannels.PROXY_INBOUND,
                Envelope.MAPPER.writeValueAsString(Envelope.make(MessageType.PING, null, "proxy", null))));
        handleInbound.invoke(server, signer.seal(RedisChannels.PROXY_INBOUND, "{\"type\":\"PING\",\"from\":123}"));
        assertTrue(receivedFrom.isEmpty());
    }

    private static String ping() throws Exception {
        return Envelope.MAPPER.writeValueAsString(Envelope.make(MessageType.PING, "lobby", "proxy", null));
    }
}
