package dev.objz.commandbridge.velocity.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.objz.commandbridge.api.message.ServerEventListener;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.velocity.net.EndpointServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

class VelocityCommandBridgeImplTest {

    @BeforeAll
    static void installLog() {
        try {
            dev.objz.commandbridge.logging.Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void requirePlayerDropsSendWhenAbsent() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions,
                tracker,
                "velocity",
                endpointServer,
                queue);

        PluginMessage payload = new PluginMessage("test", null, false, UUID.randomUUID(), null, null);

        invokeSend(bridge, Platform.BACKEND.target("backend-1"), payload).join();

        assertEquals(0, endpointServer.sendCount());
    }

    @Test
    void requirePlayerDeliversSendWhenPresent() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions,
                tracker,
                "velocity",
                endpointServer,
                queue);

        UUID player = UUID.randomUUID();
        tracker.addPlayer("backend-1", player);
        ClientSession session = sessions.add("backend-1", new TestEndpoint());
        session.location(Location.BACKEND);
        session.status(AuthStatus.AUTH_OK);

        PluginMessage payload = new PluginMessage("test", null, false, player, null, null);

        invokeSend(bridge, Platform.BACKEND.target("backend-1"), payload).join();

        assertEquals(1, endpointServer.sendCount());
    }

    @Test
    void requirePlayerFailsRequestWhenAbsent() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions,
                tracker,
                "velocity",
                endpointServer,
                queue);

        PluginMessage payload = new PluginMessage("test", null, true, UUID.randomUUID(), null, null);
        CompletableFuture<PluginMessage> future = invokeRequest(
                bridge,
                Platform.BACKEND.target("backend-1"),
                payload,
                Duration.ofSeconds(1));

        CompletionException ex = assertThrows(CompletionException.class, future::join);
        Throwable cause = assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Player not on target server: backend-1", cause.getMessage());
    }

    @Test
    void whenOnlineQueuesSendWhenAbsent() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions,
                tracker,
                "velocity",
                endpointServer,
                queue);

        UUID player = UUID.randomUUID();
        PluginMessage payload = new PluginMessage("test", null, false, null, player, null);

        invokeSend(bridge, Platform.BACKEND.target("backend-1"), payload).join();

        List<PluginMessageQueue.QueuedMessage> drained = queue.drain("backend-1", player);
        assertEquals(1, drained.size());
        assertEquals(0, endpointServer.sendCount());
    }

    @Test
    void onServerConnectedReturnsPresent() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions, tracker, "velocity", endpointServer, queue);

        Optional<dev.objz.commandbridge.api.message.Subscription> result =
                bridge.onServerConnected(server -> { });
        assertTrue(result.isPresent(), "Expected Optional.of(subscription) on Velocity");
    }

    @Test
    void onServerDisconnectedReturnsPresent() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions, tracker, "velocity", endpointServer, queue);

        Optional<dev.objz.commandbridge.api.message.Subscription> result =
                bridge.onServerDisconnected(server -> { });
        assertTrue(result.isPresent(), "Expected Optional.of(subscription) on Velocity");
    }

    @Test
    void onServerConnectedRejectsNull() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions, tracker, "velocity", endpointServer, queue);

        assertThrows(NullPointerException.class, () -> bridge.onServerConnected((ServerEventListener) null));
    }

    @Test
    void onServerConnectedSubscriptionCancels() {
        SessionHub sessions = new SessionHub();
        PlayerTracker tracker = new PlayerTracker();
        PluginMessageQueue queue = new PluginMessageQueue();
        CountingEndpointServer endpointServer = new CountingEndpointServer();
        VelocityCommandBridgeImpl bridge = new VelocityCommandBridgeImpl(
                sessions, tracker, "velocity", endpointServer, queue);

        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger();
        Optional<dev.objz.commandbridge.api.message.Subscription> sub =
                bridge.onServerConnected(server -> callCount.incrementAndGet());
        assertTrue(sub.isPresent());

        // Fire the event — listener should be called
        ClientSession session = sessions.add("srv-1", new TestEndpoint());
        session.location(dev.objz.commandbridge.scripting.model.enums.Location.BACKEND);
        session.status(dev.objz.commandbridge.security.AuthStatus.AUTH_OK);
        invokeOnServerConnected(bridge, session);
        assertEquals(1, callCount.get(), "Listener should have fired once");

        // Cancel, then fire again — listener should NOT be called
        sub.get().cancel();
        invokeOnServerConnected(bridge, session);
        assertEquals(1, callCount.get(), "Listener should not fire after cancel");
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Void> invokeSend(
            VelocityCommandBridgeImpl bridge,
            Platform.ServerTarget target,
            PluginMessage payload) {
        try {
            Method method = VelocityCommandBridgeImpl.class.getDeclaredMethod(
                    "sendPluginMessage",
                    Platform.ServerTarget.class,
                    PluginMessage.class);
            method.setAccessible(true);
            return (CompletableFuture<Void>) method.invoke(bridge, target, payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<PluginMessage> invokeRequest(
            VelocityCommandBridgeImpl bridge,
            Platform.ServerTarget target,
            PluginMessage payload,
            Duration timeout) {
        try {
            Method method = VelocityCommandBridgeImpl.class.getDeclaredMethod(
                    "requestPluginMessage",
                    Platform.ServerTarget.class,
                    PluginMessage.class,
                    Duration.class);
            method.setAccessible(true);
            return (CompletableFuture<PluginMessage>) method.invoke(bridge, target, payload, timeout);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeOnServerConnected(VelocityCommandBridgeImpl bridge, ClientSession session) {
        try {
            Method method = VelocityCommandBridgeImpl.class.getDeclaredMethod(
                    "onServerConnected",
                    ClientSession.class);
            method.setAccessible(true);
            method.invoke(bridge, session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CountingEndpointServer implements EndpointServer {
        private final AtomicInteger sendCount = new AtomicInteger();

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public SendOperation send(Endpoint endpoint, Envelope request) {
            sendCount.incrementAndGet();
            return new SendOperation(endpoint, request, new ResponseAwaiter());
        }

        @Override
        public void close(Endpoint endpoint) {
        }

        private int sendCount() {
            return sendCount.get();
        }
    }

    private static final class TestEndpoint implements Endpoint {
        @Override
        public CompletableFuture<Void> send(Envelope env) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isOpen() {
            return true;
        }
    }
}
