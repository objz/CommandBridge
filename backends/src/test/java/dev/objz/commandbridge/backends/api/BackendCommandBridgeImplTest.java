package dev.objz.commandbridge.backends.api;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.ConnectionState;
import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.enums.Location;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class BackendCommandBridgeImplTest {

    @BeforeAll
    static void installLog() {
        try {
            dev.objz.commandbridge.logging.Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
        }
    }

    private static BackendCommandBridgeImpl createBridge() {
        return new BackendCommandBridgeImpl(new StubBackendClient());
    }

    @Test
    void onServerConnectedReturnsEmpty() {
        BackendCommandBridgeImpl bridge = createBridge();
        Optional<Subscription> result = bridge.onServerConnected(server -> { });
        assertTrue(result.isEmpty(), "Expected Optional.empty() on backend platform");
    }

    @Test
    void onServerDisconnectedReturnsEmpty() {
        BackendCommandBridgeImpl bridge = createBridge();
        Optional<Subscription> result = bridge.onServerDisconnected(server -> { });
        assertTrue(result.isEmpty(), "Expected Optional.empty() on backend platform");
    }

    @Test
    void onServerConnectedRejectsNull() {
        BackendCommandBridgeImpl bridge = createBridge();
        assertThrows(NullPointerException.class, () -> bridge.onServerConnected(null));
    }

    @Test
    void onConnectionStateChangedReturnsSubscription() {
        BackendCommandBridgeImpl bridge = createBridge();
        Object result = bridge.onConnectionStateChanged(state -> { });
        assertInstanceOf(Subscription.class, result,
                "onConnectionStateChanged must return Subscription, not Optional");
        bridge.shutdown();
    }

    private static final class StubBackendClient implements BackendClient {

        @Override
        public void start() {
        }

        @Override
        public void reconnect() {
        }

        @Override
        public void scheduleReconnection() {
        }

        @Override
        public SendOperation send(Envelope request) {
            throw new UnsupportedOperationException("stub");
        }

        @Override
        public ClientStatus status() {
            return ClientStatus.DISCONNECTED;
        }

        @Override
        public String serverId() {
            return "backend-test";
        }

        @Override
        public InNode inboundRouter() {
            return new InNode();
        }

        @Override
        public OutNode outboundRouter() {
            return new OutNode();
        }

        @Override
        public void setLocation(Location location) {
        }

        @Override
        public void setServerId(String serverId) {
        }

        @Override
        public void onAuthenticated(Runnable callback) {
        }

        @Override
        public void close() {
        }
    }
}
