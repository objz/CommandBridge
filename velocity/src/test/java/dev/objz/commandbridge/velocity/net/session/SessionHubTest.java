package dev.objz.commandbridge.velocity.net.session;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionHubTest {

    private static final class TestEndpoint implements Endpoint {
        private final boolean open;

        TestEndpoint() {
            this(true);
        }

        TestEndpoint(boolean open) {
            this.open = open;
        }

        @Override
        public CompletableFuture<Void> send(Envelope env) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isOpen() {
            return open;
        }
    }

    private SessionHub hub;

    @BeforeAll
    static void installLog() {
        try {
            Log.install(java.util.logging.Logger.getLogger("SessionHubTest"));
        } catch (IllegalStateException e) {
            // Log already installed, ignore
        }
    }

    @BeforeEach
    void setUp() {
        hub = new SessionHub();
    }

    @Test
    void addCreatesSession() {
        Endpoint ep = new TestEndpoint();
        ClientSession result = hub.add("s1", ep);

        assertNotNull(result);
        assertEquals("s1", result.id());
    }

    @Test
    void getByIdReturnsSession() {
        Endpoint ep = new TestEndpoint();
        hub.add("s1", ep);

        assertTrue(hub.get("s1").isPresent());
    }

    @Test
    void getByIdReturnsEmptyForUnknown() {
        assertFalse(hub.get("unknown").isPresent());
    }

    @Test
    void getByEndpointReturnsSession() {
        Endpoint ep = new TestEndpoint();
        hub.add("s1", ep);

        ClientSession result = hub.get(ep);
        assertNotNull(result);
    }

    @Test
    void addDuplicateIdReplacesOld() {
        Endpoint ep1 = new TestEndpoint();
        Endpoint ep2 = new TestEndpoint();

        hub.add("s1", ep1);
        hub.add("s1", ep2);

        assertNull(hub.get(ep1));
    }

    @Test
    void removeByIdCleansUpEndpoint() {
        Endpoint ep = new TestEndpoint();
        hub.add("s1", ep);

        hub.remove("s1");

        assertNull(hub.get(ep));
    }

    @Test
    void removeByEndpointCleansUpId() {
        Endpoint ep = new TestEndpoint();
        hub.add("s1", ep);

        hub.remove(ep);

        assertFalse(hub.get("s1").isPresent());
    }

    @Test
    void removeFiresListener() {
        AtomicInteger count = new AtomicInteger(0);
        hub.onRemove(session -> count.incrementAndGet());

        Endpoint ep = new TestEndpoint();
        hub.add("s1", ep);
        hub.remove("s1");

        assertEquals(1, count.get());
    }

    @Test
    void clearRemovesAll() {
        Endpoint ep1 = new TestEndpoint();
        Endpoint ep2 = new TestEndpoint();

        hub.add("s1", ep1);
        hub.add("s2", ep2);

        hub.clear();

        assertEquals(0, hub.size());
    }

    @Test
    void sizeReturnsCount() {
        Endpoint ep1 = new TestEndpoint();
        Endpoint ep2 = new TestEndpoint();
        Endpoint ep3 = new TestEndpoint();

        hub.add("s1", ep1);
        hub.add("s2", ep2);
        hub.add("s3", ep3);

        assertEquals(3, hub.size());
    }

    @Test
    void findSessionRequiresAuthOk() {
        Endpoint ep = new TestEndpoint();
        ClientSession session = hub.add("s1", ep);
        // status defaults to AUTH_FAIL

        assertFalse(hub.findSession("s1", Location.BACKEND).isPresent());
    }

    @Test
    void findSessionRequiresOpenEndpoint() {
        Endpoint ep = new TestEndpoint(false);
        ClientSession session = hub.add("s1", ep);
        session.status(AuthStatus.AUTH_OK);

        assertFalse(hub.findSession("s1", Location.BACKEND).isPresent());
    }

    @Test
    void findSessionRequiresMatchingLocation() {
        Endpoint ep = new TestEndpoint();
        ClientSession session = hub.add("s1", ep);
        session.status(AuthStatus.AUTH_OK);
        session.location(Location.BACKEND);

        assertFalse(hub.findSession("s1", Location.VELOCITY).isPresent());
    }

    @Test
    void addWithNullIdGeneratesUUID() {
        Endpoint ep = new TestEndpoint();
        ClientSession result = hub.add(null, ep);

        assertTrue(result.id().startsWith("unknown-"));
    }
}
