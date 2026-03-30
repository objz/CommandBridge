package dev.objz.commandbridge.net.proto;

import dev.objz.commandbridge.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Envelope}.
 * Verifies factory method behavior, ID uniqueness, and JSON roundtrip correctness.
 */
final class EnvelopeTest {

    @BeforeAll
    static void setUp() {
        TestFixtures.ensureLog();
    }

    @Test
    void makeGeneratesUniqueId() {
        Envelope a = Envelope.make(MessageType.PING, "proxy", "backend", null);
        Envelope b = Envelope.make(MessageType.PING, "proxy", "backend", null);
        assertNotEquals(a.id(), b.id(), "Consecutive make() calls must produce different IDs");
    }

    @Test
    void makeSetsCorrectType() {
        Envelope env = Envelope.make(MessageType.EXECUTE_COMMAND, "proxy", "backend", null);
        assertEquals(MessageType.EXECUTE_COMMAND, env.type());
    }

    @Test
    void makeSetsTimestamp() {
        long before = System.currentTimeMillis();
        Envelope env = Envelope.make(MessageType.PING, "proxy", "backend", null);
        long after = System.currentTimeMillis();
        assertTrue(env.ts() >= before && env.ts() <= after,
                "Timestamp should be between before and after currentTimeMillis");
    }

    @Test
    void replyReusesRequestId() {
        Envelope request = Envelope.make(MessageType.PING, "proxy", "backend", null);
        Envelope reply = Envelope.reply(request, MessageType.PONG, "backend", null);
        assertEquals(request.id(), reply.id(), "Reply must reuse the request ID for correlation");
    }

    @Test
    void replySetsCorrectType() {
        Envelope request = Envelope.make(MessageType.PING, "proxy", "backend", null);
        Envelope reply = Envelope.reply(request, MessageType.PONG, "backend", null);
        assertEquals(MessageType.PONG, reply.type());
    }

    @Test
    void serializationRoundtripPreservesFields() throws Exception {
        Envelope original = Envelope.make(MessageType.EXECUTE_COMMAND, "proxy", "backend-1", null);
        String json = Envelope.MAPPER.writeValueAsString(original);
        Envelope restored = Envelope.MAPPER.readValue(json, Envelope.class);

        assertEquals(original.v(), restored.v());
        assertEquals(original.id(), restored.id());
        assertEquals(original.type(), restored.type());
        assertEquals(original.from(), restored.from());
        assertEquals(original.to(), restored.to());
        assertEquals(original.ts(), restored.ts());
        assertEquals(original.payload(), restored.payload());
    }
}
