package dev.objz.commandbridge.net;

import dev.objz.commandbridge.TestFixtures;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ResponseAwaiter}.
 * Verifies signal matching by client ID, wildcard null-clientId fallback,
 * and safe behavior when no awaiter is pending.
 */
final class ResponseAwaiterTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    private static Envelope envelope(UUID id, String from) {
        return new Envelope(1, id, MessageType.PONG, from, "proxy",
                System.currentTimeMillis(), Envelope.MAPPER.nullNode());
    }

    @Test
    void awaitThenSignalCompletesFuture() throws Exception {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID id = UUID.randomUUID();
        CompletableFuture<Envelope> future = awaiter.await(
                null, id, e -> true, Duration.ofSeconds(5));

        awaiter.signal(envelope(id, "server1"));

        Envelope result = future.get(1, TimeUnit.SECONDS);
        assertEquals(id, result.id());
    }

    @Test
    void signalMatchingClientIdCompletesFuture() throws Exception {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID id = UUID.randomUUID();
        CompletableFuture<Envelope> future = awaiter.await(
                "server1", id, e -> true, Duration.ofSeconds(5));

        awaiter.signal(envelope(id, "server1"));

        Envelope result = future.get(1, TimeUnit.SECONDS);
        assertEquals(id, result.id());
    }

    @Test
    void signalNonMatchingClientIdDoesNotComplete() {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID id = UUID.randomUUID();
        CompletableFuture<Envelope> future = awaiter.await(
                "server1", id, e -> true, Duration.ofSeconds(5));

        boolean handled = awaiter.signal(envelope(id, "server2"));

        assertFalse(handled, "Signal from wrong clientId should not be handled");
        assertFalse(future.isDone(), "Future should remain incomplete");
    }

    @Test
    void signalNullClientIdMatchesAnyAwaiter() throws Exception {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID id = UUID.randomUUID();
        CompletableFuture<Envelope> future = awaiter.await(
                null, id, e -> true, Duration.ofSeconds(5));

        boolean handled = awaiter.signal(envelope(id, "any-server"));

        assertTrue(handled, "Null-clientId awaiter should match signal from any client");
        Envelope result = future.get(1, TimeUnit.SECONDS);
        assertEquals(id, result.id());
    }

    @Test
    void signalNoMatchingAwaiterDoesNotThrow() {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        boolean handled = awaiter.signal(envelope(UUID.randomUUID(), "server1"));
        assertFalse(handled, "Signal with no pending awaiter should return false");
    }
}
