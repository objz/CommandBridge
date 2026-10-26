package dev.objz.commandbridge.backends.net.client;

import dev.objz.commandbridge.backends.TestFixtures;
import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.backends.net.out.AuthRequest;
import dev.objz.commandbridge.backends.net.routing.AuthHandler;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.ScheduleHandle;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.AuthRequestPayload;
import dev.objz.commandbridge.net.payloads.util.AuthResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WsClientAuthenticationTest {
    private final BackendsConfig config = BackendsConfig.defaults();
    private final AuthService proof = new AuthService(config.security().secret());
    private final ResponseAwaiter awaiter = new ResponseAwaiter();
    private final List<Envelope> requests = new ArrayList<>();
    private final List<CompletableFuture<Void>> sends = new ArrayList<>();
    private final AtomicInteger authenticated = new AtomicInteger();
    private final AtomicInteger reconnects = new AtomicInteger();
    private WsClient client;
    private AuthHandler authentication;

    @BeforeAll
    static void setupLog() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setup() throws Exception {
        PlatformAdapter<?> adapter = (PlatformAdapter<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { PlatformAdapter.class }, (self, method, args) -> {
                    if (method.getName().equals("runSchedule")) {
                        reconnects.incrementAndGet();
                        return (ScheduleHandle) () -> { };
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        client = new WsClient(config, Path.of("."), adapter);
        Endpoint endpoint = new Endpoint() {
            public CompletableFuture<Void> send(Envelope request) {
                requests.add(request);
                CompletableFuture<Void> sent = new CompletableFuture<>();
                sends.add(sent);
                return sent;
            }

            public boolean isOpen() {
                return true;
            }
        };
        client.outboundRouter().setSendOperationFactory(env -> new SendOperation(endpoint, env, awaiter));
        client.outboundRouter().register(MessageType.AUTH_REQUEST, new AuthRequest(proof, Location.BACKEND));
        client.onAuthenticated(authenticated::incrementAndGet);
        Field handler = WsClient.class.getDeclaredField("authHandler");
        handler.setAccessible(true);
        authentication = (AuthHandler) handler.get(client);
    }

    @AfterEach
    void close() throws Exception {
        client.close();
    }

    @Test
    void successfulProofAfterCloseCannotResurrectTheClient() throws Exception {
        authentication.authenticate();
        client.close();

        succeed(0);

        assertEquals(ClientStatus.DISCONNECTED, client.status());
        assertEquals(0, authenticated.get());
        assertEquals(0, reconnects.get());
    }

    @Test
    void rejectionAfterCloseCannotScheduleReconnect() throws Exception {
        authentication.authenticate();
        client.close();

        reply(0, MessageType.AUTH_FAIL, null);

        assertEquals(ClientStatus.DISCONNECTED, client.status());
        assertEquals(0, reconnects.get());
    }

    @Test
    void currentRejectionStillSchedulesReconnect() {
        authentication.authenticate();

        reply(0, MessageType.AUTH_FAIL, null);

        assertEquals(ClientStatus.DISCONNECTED, client.status());
        assertEquals(1, reconnects.get());
    }

    @Test
    void oldSuccessfulProofCannotAuthenticateAReplacementAttempt() throws Exception {
        authentication.authenticate();
        authentication.authenticate();

        succeed(0);
        assertEquals(0, authenticated.get());
        succeed(1);

        assertEquals(ClientStatus.AUTH_OK, client.status());
        assertEquals(1, authenticated.get());
    }

    @Test
    void oldRejectionCannotDisconnectAnAuthenticatedReplacement() throws Exception {
        authentication.authenticate();
        authentication.authenticate();
        succeed(1);

        reply(0, MessageType.AUTH_FAIL, null);

        assertEquals(ClientStatus.AUTH_OK, client.status());
        assertEquals(1, authenticated.get());
        assertEquals(0, reconnects.get());
    }

    @Test
    void oldTimeoutCannotDisconnectAnAuthenticatedReplacement() throws Exception {
        authentication.authenticate();
        authentication.authenticate();
        succeed(1);

        sends.getFirst().completeExceptionally(new TimeoutException("old connection"));

        assertEquals(ClientStatus.AUTH_OK, client.status());
        assertEquals(0, reconnects.get());
    }

    @Test
    void timeoutAndTransportCloseAfterCloseAreIgnored() throws Exception {
        authentication.authenticate();
        client.close();

        sends.getFirst().completeExceptionally(new TimeoutException("closed connection"));
        Method connectionLost = WsClient.class.getDeclaredMethod("onConnectionLost", long.class);
        connectionLost.setAccessible(true);
        connectionLost.invoke(client, 0L);

        assertEquals(ClientStatus.DISCONNECTED, client.status());
        assertEquals(0, reconnects.get());
    }

    @Test
    void transportCloseFromAnOlderGenerationCannotDisconnectTheReplacement() throws Exception {
        Field generation = WsClient.class.getDeclaredField("connectionGeneration");
        generation.setAccessible(true);
        generation.setLong(client, 2L);
        authentication.authenticate();
        succeed(0);

        Method connectionLost = WsClient.class.getDeclaredMethod("onConnectionLost", long.class);
        connectionLost.setAccessible(true);
        connectionLost.invoke(client, 1L);

        assertEquals(ClientStatus.AUTH_OK, client.status());
        assertEquals(0, reconnects.get());
    }

    private void succeed(int index) throws Exception {
        Envelope request = requests.get(index);
        String nonce = Envelope.MAPPER.treeToValue(request.payload(), AuthRequestPayload.class).clientNonce();
        reply(index, MessageType.AUTH_OK, new AuthResponsePayload("server-nonce",
                proof.signServerProof(config.clientId(), nonce, "server-nonce")));
    }

    private void reply(int index, MessageType type, Object payload) {
        Envelope request = requests.get(index);
        assertTrue(awaiter.signal(Envelope.reply(request, type, request.to(),
                payload == null ? null : Envelope.MAPPER.valueToTree(payload))));
    }
}
