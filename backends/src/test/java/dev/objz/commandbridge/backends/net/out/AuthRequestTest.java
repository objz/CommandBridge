package dev.objz.commandbridge.backends.net.out;

import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.backends.net.out.ctx.AuthRequestContext;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.AuthRequestPayload;
import dev.objz.commandbridge.net.payloads.util.AuthResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuthRequestTest {

    private static final String CLIENT_ID = "survival-1";

    private final AuthService auth = new AuthService("shared-secret");
    private final ResponseAwaiter awaiter = new ResponseAwaiter();
    private final List<Envelope> sent = new CopyOnWriteArrayList<>();
    private final List<ClientStatus> statuses = new CopyOnWriteArrayList<>();
    private final AtomicInteger authFailedCalls = new AtomicInteger();
    private OutNode outNode;

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setUp() {
        Endpoint endpoint = new Endpoint() {
            @Override
            public CompletableFuture<Void> send(Envelope env) {
                sent.add(env);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        };
        outNode = new OutNode()
                .setSendOperationFactory(env -> new SendOperation(endpoint, env, awaiter))
                .setClientId(CLIENT_ID);
        outNode.register(MessageType.AUTH_REQUEST, new AuthRequest(auth, Location.BACKEND));
    }

    private Envelope start(Duration timeout) {
        outNode.send(MessageType.AUTH_REQUEST,
                new AuthRequestContext(timeout, statuses::add, authFailedCalls::incrementAndGet));
        assertEquals(1, sent.size());
        return sent.getFirst();
    }

    private void reply(Envelope request, MessageType type, Object payload) {
        Envelope reply = Envelope.reply(request, type, request.to(),
                payload == null ? null : Envelope.MAPPER.valueToTree(payload));
        assertTrue(awaiter.signal(reply), "reply should match the pending auth request");
    }

    private static String clientNonce(Envelope request) throws Exception {
        return Envelope.MAPPER.treeToValue(request.payload(), AuthRequestPayload.class).clientNonce();
    }

    private void assertFailedAndReconnectTriggered() {
        assertEquals(List.of(ClientStatus.AUTH_FAILED), statuses);
        assertEquals(1, authFailedCalls.get(), "onAuthFailed should run so the client reconnects");
    }

    @Test
    void requestCarriesSignedNonceAndLocation() throws Exception {
        Envelope request = start(Duration.ofSeconds(5));

        assertEquals(MessageType.AUTH_REQUEST, request.type());
        assertEquals(CLIENT_ID, request.from());
        assertEquals("proxy-auth", request.to());
        var payload = Envelope.MAPPER.treeToValue(request.payload(), AuthRequestPayload.class);
        assertEquals(Location.BACKEND, payload.location());
        assertTrue(auth.verify(CLIENT_ID, payload.clientNonce(), payload.hmac()));
    }

    @Test
    void validServerProofAuthenticates() throws Exception {
        Envelope request = start(Duration.ofSeconds(5));
        String serverNonce = "server-nonce";

        reply(request, MessageType.AUTH_OK, new AuthResponsePayload(serverNonce,
                auth.signServerProof(CLIENT_ID, clientNonce(request), serverNonce)));

        assertEquals(List.of(ClientStatus.AUTH_OK), statuses);
        assertEquals(0, authFailedCalls.get());
    }

    @Test
    void badServerProofFails() {
        Envelope request = start(Duration.ofSeconds(5));

        reply(request, MessageType.AUTH_OK, new AuthResponsePayload("server-nonce",
                new AuthService("other-secret").signServerProof(CLIENT_ID, "x", "server-nonce")));

        assertFailedAndReconnectTriggered();
    }

    @Test
    void authFailReplyFails() {
        Envelope request = start(Duration.ofSeconds(5));

        reply(request, MessageType.AUTH_FAIL, null);

        assertFailedAndReconnectTriggered();
    }

    @Test
    void malformedReplyFails() {
        Envelope request = start(Duration.ofSeconds(5));

        reply(request, MessageType.AUTH_OK, "not an object");

        assertFailedAndReconnectTriggered();
    }

    @Test
    void timeoutFails() throws Exception {
        start(Duration.ofMillis(50));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (authFailedCalls.get() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }

        assertFailedAndReconnectTriggered();
    }

    @Test
    void unrelatedReplyIsIgnored() {
        Envelope request = start(Duration.ofSeconds(5));

        Envelope other = Envelope.reply(request, MessageType.PONG, request.to(), null);

        assertFalse(awaiter.signal(other));
        assertTrue(statuses.isEmpty());
    }
}
