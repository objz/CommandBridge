package dev.objz.commandbridge.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseAwaiterTest {

    @BeforeAll
    static void installLog() {
        try {
            Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) {
            // already installed
        }
    }

    private static Envelope envelope(UUID id, String from) {
        return new Envelope(1, id, MessageType.PING, from, "server1",
                System.currentTimeMillis(), Envelope.MAPPER.nullNode());
    }

    @Test
    void signalMatchesById() throws Exception {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID testId = UUID.randomUUID();

        CompletableFuture<Envelope> future =
                awaiter.await("client1", testId, env -> true, Duration.ofMillis(500));

        Envelope env = envelope(testId, "client1");
        boolean consumed = awaiter.signal(env);

        assertTrue(consumed);
        assertEquals(env, future.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void signalNonMatchingIdIgnored() {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID awaitId = UUID.randomUUID();
        UUID signalId = UUID.randomUUID();

        CompletableFuture<Envelope> future =
                awaiter.await("client1", awaitId, env -> true, Duration.ofMillis(500));

        Envelope env = envelope(signalId, "client1");
        boolean consumed = awaiter.signal(env);

        assertFalse(consumed);
        assertFalse(future.isDone());
    }

    @Test
    void timeoutCompletesExceptionally() {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID testId = UUID.randomUUID();

        CompletableFuture<Envelope> future =
                awaiter.await("client1", testId, env -> true, Duration.ofMillis(50));

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> future.get(300, TimeUnit.MILLISECONDS));

        assertInstanceOf(TimeoutException.class, ex.getCause());
    }

    @Test
    void doubleSignalReturnsFalse() throws Exception {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID testId = UUID.randomUUID();

        awaiter.await("client1", testId, env -> true, Duration.ofMillis(500));

        Envelope env = envelope(testId, "client1");
        boolean first = awaiter.signal(env);
        boolean second = awaiter.signal(env);

        assertTrue(first);
        assertFalse(second);
    }

    @Test
    void nullClientIdFallback() throws Exception {
        ResponseAwaiter awaiter = new ResponseAwaiter();
        UUID testId = UUID.randomUUID();

        CompletableFuture<Envelope> future =
                awaiter.await(null, testId, env -> true, Duration.ofMillis(500));

        Envelope env = envelope(testId, "anyClient");
        boolean consumed = awaiter.signal(env);

        assertTrue(consumed);
        assertEquals(env, future.get(100, TimeUnit.MILLISECONDS));
    }
}
