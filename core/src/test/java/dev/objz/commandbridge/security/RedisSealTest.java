package dev.objz.commandbridge.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests Redis authentication, routing, and replay protection. */
final class RedisSealTest {

    private static final String CHANNEL = "commandbridge:proxy:in";
    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final RedisSeal proxy = new RedisSeal("s3cr3t", now::get);
    private final RedisSeal backend = new RedisSeal("s3cr3t", now::get);

    @Test
    void theOtherSideOpensWhatOneSideSealed() {
        assertEquals("{\"type\":\"PING\"}", backend.open(CHANNEL, proxy.seal(CHANNEL, "{\"type\":\"PING\"}")));
    }

    @Test
    void anotherSecretCanNotOpenIt() {
        assertNull(new RedisSeal("other", now::get).open(CHANNEL, proxy.seal(CHANNEL, "{}")));
    }

    @Test
    void aChangedMessageIsRefused() {
        String sealed = proxy.seal(CHANNEL, "{\"type\":\"PING\"}");
        assertNull(backend.open(CHANNEL, sealed.replace("PING", "EXECUTE_COMMAND")));
    }

    @Test
    void aMessageIsAcceptedOnce() {
        String sealed = proxy.seal(CHANNEL, "{}");
        assertEquals("{}", backend.open(CHANNEL, sealed));
        assertNull(backend.open(CHANNEL, sealed));
    }

    @Test
    void anOldMessageIsRefused() {
        String sealed = proxy.seal(CHANNEL, "{}");
        now.addAndGet(RedisSeal.WINDOW_MS + 1);
        assertNull(backend.open(CHANNEL, sealed));
    }

    @Test
    void aMessageFromTooFarAheadIsRefused() {
        String sealed = new RedisSeal("s3cr3t", () -> now.get() + RedisSeal.WINDOW_MS + 1).seal(CHANNEL, "{}");
        assertNull(backend.open(CHANNEL, sealed));
    }

    @Test
    void plainOrBrokenInputIsRefused() {
        assertNull(backend.open(CHANNEL, "{\"type\":\"EXECUTE_COMMAND\",\"from\":\"proxy\"}"));
        assertNull(backend.open(CHANNEL, "not json"));
        assertNull(backend.open(CHANNEL, "{\"t\":\"x\",\"n\":1,\"m\":2,\"s\":3}"));
        assertNull(backend.open(CHANNEL, "null"));
        assertNull(backend.open(CHANNEL, null));
    }

    @Test
    void cannotReflectMessagesToAnotherChannelOrDirection() {
        String sealed = proxy.seal("commandbridge:client:lobby", "{}");
        assertNull(backend.open("commandbridge:client:survival", sealed));
        assertNull(backend.open(CHANNEL, sealed));
        assertNull(backend.open("commandbridge:clients:control", sealed));
        assertEquals("{}", backend.open("commandbridge:client:lobby", sealed));
    }

    @Test
    void tamperedTimestampNonceAndSignatureAreRefused() {
        String sealed = proxy.seal(CHANNEL, "{}");
        assertNull(backend.open(CHANNEL, sealed.replace("1000000", "1000001")));
        assertNull(backend.open(CHANNEL, sealed.replace("\"n\":\"", "\"n\":\"a")));
        assertNull(backend.open(CHANNEL, sealed.replace("\"s\":\"", "\"s\":\"a")));
        assertEquals("{}", backend.open(CHANNEL, sealed));
    }

    @Test
    void timestampArithmeticCannotWrapIntoTheWindow() {
        String sealed = new RedisSeal("s3cr3t", () -> Long.MIN_VALUE + now.get()).seal(CHANNEL, "{}");
        assertNull(backend.open(CHANNEL, sealed));
        String tooLarge = proxy.seal(CHANNEL, "{}").replace("1000000", "18446744073710551616");
        assertNull(backend.open(CHANNEL, tooLarge));
    }

    @Test
    void clockSkewIsAllowedAtBothWindowEdges() {
        String behind = new RedisSeal("s3cr3t", () -> now.get() - RedisSeal.WINDOW_MS).seal(CHANNEL, "{}");
        String ahead = new RedisSeal("s3cr3t", () -> now.get() + RedisSeal.WINDOW_MS).seal(CHANNEL, "{}");
        assertEquals("{}", backend.open(CHANNEL, behind));
        assertEquals("{}", backend.open(CHANNEL, ahead));
    }

    @Test
    void futureDatedNonceStaysRememberedUntilItsMessageExpires() {
        String ahead = new RedisSeal("s3cr3t", () -> now.get() + RedisSeal.WINDOW_MS).seal(CHANNEL, "{}");
        assertEquals("{}", backend.open(CHANNEL, ahead));
        now.addAndGet(2 * RedisSeal.WINDOW_MS);
        assertNull(backend.open(CHANNEL, ahead));
    }

    @Test
    void concurrentDeliveryIsAcceptedOnlyOnce() {
        String sealed = proxy.seal(CHANNEL, "{}");
        long accepted = IntStream.range(0, 32).parallel()
                .filter(i -> backend.open(CHANNEL, sealed) != null).count();
        assertEquals(1, accepted);
    }

    @Test
    void oversizedInputAndOutputAreRefused() {
        String oversized = "x".repeat(RedisSeal.MAX_MESSAGE_CHARS + 1);
        assertNull(backend.open(CHANNEL, oversized));
        assertThrows(IllegalArgumentException.class, () -> proxy.seal(CHANNEL, oversized));
    }

    @Test
    void malformedUnicodeCannotReplaceAuthenticatedQuestionMark() {
        String message = "{\"command\":\"say ?\"}";
        String sealed = proxy.seal(CHANNEL, message);
        assertNull(backend.open(CHANNEL, sealed.replace("?", "\\ud800")));
        assertEquals(message, backend.open(CHANNEL, sealed));
        assertThrows(IllegalArgumentException.class,
                () -> proxy.seal(CHANNEL, message.replace('?', (char) 0xd800)));
    }

    @Test
    void validUnicodeIncludingSurrogatePairsRoundTrips() {
        String message = "{\"command\":\"say 你好 😀\"}";
        assertEquals(message, backend.open(CHANNEL, proxy.seal(CHANNEL, message)));
    }

    @Test
    void fullReplayCacheRefusesMessagesWithoutEvictingLiveNonces() {
        String first = proxy.seal(CHANNEL, "{}");
        assertEquals("{}", backend.open(CHANNEL, first));
        for (int i = 1; i < RedisSeal.MAX_SEEN; i++) {
            assertEquals("{}", backend.open(CHANNEL, proxy.seal(CHANNEL, "{}")));
        }
        assertNull(backend.open(CHANNEL, proxy.seal(CHANNEL, "{}")));
        assertNull(backend.open(CHANNEL, first));
        now.addAndGet(RedisSeal.WINDOW_MS + 1);
        assertEquals("{}", backend.open(CHANNEL, proxy.seal(CHANNEL, "{}")));
    }
}
