package dev.objz.commandbridge.velocity.net.in;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.AuthRequestPayload;
import dev.objz.commandbridge.net.payloads.util.AuthResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.net.EndpointServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuthHandlerTest {

    private static final String SECRET = "test-secret";

    static final class FakeEndpoint implements Endpoint {
        final List<Envelope> sent = new ArrayList<>();

        @Override
        public CompletableFuture<Void> send(Envelope env) {
            sent.add(env);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isOpen() {
            return true;
        }
    }

    static final class FakeServer implements EndpointServer {
        final List<Endpoint> closed = new ArrayList<>();

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public SendOperation send(Endpoint endpoint, Envelope request) {
            return new SendOperation(endpoint, request, new ResponseAwaiter());
        }

        @Override
        public void close(Endpoint endpoint) {
            closed.add(endpoint);
        }
    }

    private final AuthService auth = new AuthService(SECRET);
    private SessionHub sessions;
    private FakeServer server;
    private AuthHandler handler;

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setUp() {
        sessions = new SessionHub();
        server = new FakeServer();
        handler = new AuthHandler(auth, sessions, server);
        handler.setSendOperationFactory(server::send);
    }

    private Envelope request(String clientId, AuthRequestPayload payload) {
        return Envelope.make(MessageType.AUTH_REQUEST, clientId, "proxy-1",
                payload == null ? null : Envelope.MAPPER.valueToTree(payload));
    }

    private AuthRequestPayload signed(String clientId, String nonce, Location location) {
        return new AuthRequestPayload(location, nonce, auth.sign(clientId, nonce));
    }

    private static Envelope onlyReply(FakeEndpoint endpoint) {
        assertEquals(1, endpoint.sent.size(), "expected exactly one reply");
        return endpoint.sent.getFirst();
    }

    @Test
    void emptyPayloadIsRejectedAndClosed() {
        var endpoint = new FakeEndpoint();

        handler.accept(endpoint, request("survival-1", null));

        assertEquals(MessageType.AUTH_FAIL, onlyReply(endpoint).type());
        assertEquals(List.of(endpoint), server.closed);
        assertEquals(0, sessions.size());
    }

    @Test
    void missingNonceIsRejectedAndClosed() {
        var endpoint = new FakeEndpoint();

        handler.accept(endpoint, request("survival-1", new AuthRequestPayload(Location.BACKEND, null, "mac")));

        assertEquals(MessageType.AUTH_FAIL, onlyReply(endpoint).type());
        assertEquals(List.of(endpoint), server.closed);
        assertEquals(0, sessions.size());
    }

    @Test
    void wrongMessageTypeIsClosedWithoutReply() {
        var endpoint = new FakeEndpoint();
        Envelope ping = Envelope.make(MessageType.PING, "survival-1", "proxy-1", null);

        handler.accept(endpoint, ping);

        assertTrue(endpoint.sent.isEmpty());
        assertEquals(List.of(endpoint), server.closed);
    }

    @Test
    void badHmacIsRejectedWithoutSession() {
        var endpoint = new FakeEndpoint();
        var forged = new AuthRequestPayload(Location.BACKEND, "nonce",
                new AuthService("other-secret").sign("survival-1", "nonce"));

        handler.accept(endpoint, request("survival-1", forged));

        assertEquals(MessageType.AUTH_FAIL, onlyReply(endpoint).type());
        assertEquals(List.of(endpoint), server.closed);
        assertTrue(sessions.get("survival-1").isEmpty());
    }

    @Test
    void validRequestCreatesSessionAndRepliesWithVerifiableProof() throws Exception {
        var endpoint = new FakeEndpoint();
        Envelope req = request("survival-1", signed("survival-1", "client-nonce", Location.VELOCITY));

        handler.accept(endpoint, req);

        Envelope reply = onlyReply(endpoint);
        assertEquals(MessageType.AUTH_OK, reply.type());
        assertEquals(req.id(), reply.id());
        assertEquals("survival-1", reply.to());
        var proof = Envelope.MAPPER.treeToValue(reply.payload(), AuthResponsePayload.class);
        assertTrue(auth.verifyServerProof("survival-1", "client-nonce", proof.serverNonce(), proof.hmac()));
        assertTrue(server.closed.isEmpty());

        ClientSession session = sessions.get("survival-1").orElseThrow();
        assertSame(endpoint, session.endpoint());
        assertEquals(AuthStatus.AUTH_OK, session.status());
        assertEquals(Location.VELOCITY, session.location());
    }

    @Test
    void missingLocationDefaultsToBackend() {
        handler.accept(new FakeEndpoint(), request("survival-1", signed("survival-1", "n", null)));

        assertEquals(Location.BACKEND, sessions.get("survival-1").orElseThrow().location());
    }

    @Test
    void reconnectFromNewEndpointClosesOldEndpoint() {
        var oldEndpoint = new FakeEndpoint();
        var newEndpoint = new FakeEndpoint();
        handler.accept(oldEndpoint, request("survival-1", signed("survival-1", "n1", Location.BACKEND)));

        handler.accept(newEndpoint, request("survival-1", signed("survival-1", "n2", Location.BACKEND)));

        assertEquals(List.of(oldEndpoint), server.closed);
        assertSame(newEndpoint, sessions.get("survival-1").orElseThrow().endpoint());
        assertEquals(1, sessions.size());
    }

    @Test
    void reauthOnSameEndpointDoesNotCloseIt() {
        var endpoint = new FakeEndpoint();
        handler.accept(endpoint, request("survival-1", signed("survival-1", "n1", Location.BACKEND)));

        handler.accept(endpoint, request("survival-1", signed("survival-1", "n2", Location.BACKEND)));

        assertTrue(server.closed.isEmpty());
    }

    @Test
    void listenerIsNotifiedWithNewSession() {
        List<ClientSession> notified = new ArrayList<>();
        handler.onAuthenticated(notified::add);

        handler.accept(new FakeEndpoint(), request("survival-1", signed("survival-1", "n", Location.BACKEND)));

        assertEquals(1, notified.size());
        assertEquals("survival-1", notified.getFirst().id());
    }

    @Test
    void throwingListenerDoesNotBreakAuthentication() {
        handler.onAuthenticated(s -> {
            throw new IllegalStateException("boom");
        });
        var endpoint = new FakeEndpoint();

        handler.accept(endpoint, request("survival-1", signed("survival-1", "n", Location.BACKEND)));

        assertEquals(MessageType.AUTH_OK, onlyReply(endpoint).type());
        assertFalse(sessions.get("survival-1").isEmpty());
    }
}
