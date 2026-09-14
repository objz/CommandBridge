package dev.objz.commandbridge.backends.net.routing;

import dev.objz.commandbridge.api.platform.ConnectionState;
import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.backends.net.out.AuthRequest;
import dev.objz.commandbridge.backends.net.out.ctx.AuthRequestContext;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RedisAuthenticationTest {

    private static final Endpoint ENDPOINT = new Endpoint() {
        @Override
        public CompletableFuture<Void> send(Envelope env) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isOpen() {
            return true;
        }
    };

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void authenticationRequiredReauthenticatesAnAuthenticatedRedisClientOnce() throws Exception {
        InNode inNode = new InNode();
        AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.AUTHENTICATED);
        RedisMessageRouter router = new RedisMessageRouter(
                inNode, new OutNode(), new ResponseAwaiter(), state, "secret", Location.BACKEND);
        AtomicInteger attempts = new AtomicInteger();
        router.onAuthenticationRequired(attempts::incrementAndGet);
        router.setupEndpoint(ENDPOINT);

        String message = Envelope.MAPPER.writeValueAsString(
                Envelope.make(MessageType.AUTH_REQUIRED, "proxy-auth", "*", null));
        router.onText(ENDPOINT, message);
        router.onText(ENDPOINT, message);

        assertEquals(1, attempts.get());
        assertEquals(ConnectionState.CONNECTED, state.get());
    }

    @Test
    void authenticationTimeoutRequestsReconnect() throws Exception {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        OutNode outNode = new OutNode()
                .setClientId("backend")
                .setSendOperationFactory(env -> new SendOperation(ENDPOINT, env, awaiter));
        outNode.register(MessageType.AUTH_REQUEST, new AuthRequest(new AuthService("secret"), Location.BACKEND));
        CountDownLatch reconnect = new CountDownLatch(1);
        AtomicReference<ClientStatus> status = new AtomicReference<>();

        outNode.send(MessageType.AUTH_REQUEST,
                new AuthRequestContext(Duration.ofMillis(20), status::set, reconnect::countDown));

        assertTrue(reconnect.await(1, TimeUnit.SECONDS));
        assertEquals(ClientStatus.AUTH_FAILED, status.get());
    }
}
