package dev.objz.commandbridge.velocity.api;

import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.velocity.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PluginMessageQueueTest {

    private static final long TTL = TimeUnit.MINUTES.toMillis(5);
    private static final UUID ALEX = UUID.randomUUID();
    private static final UUID STEVE = UUID.randomUUID();
    private static final Platform.ServerTarget SURVIVAL = Platform.backend("Survival-1");
    private static final Platform.ServerTarget LOBBY = Platform.backend("lobby");

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final PluginMessageQueue queue = new PluginMessageQueue(now::get);

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    private static PluginMessage message(String text) {
        return new PluginMessage("test.Channel", Envelope.MAPPER.valueToTree(text), false,
                null, ALEX, null);
    }

    private static String text(PluginMessageQueue.QueuedMessage queued) {
        return queued.message().data().asText();
    }

    @Test
    void queuedMessageLosesWhenOnlineButKeepsTheRest() {
        UUID required = UUID.randomUUID();
        var original = new PluginMessage("test.Channel", Envelope.MAPPER.valueToTree("hi"), true,
                required, ALEX, null);

        queue.queue(ALEX, SURVIVAL, original, "proxy-2");
        var drained = queue.drain("survival-1", ALEX);

        assertEquals(1, drained.size());
        PluginMessage stored = drained.getFirst().message();
        assertNull(stored.whenOnline());
        assertEquals(required, stored.requirePlayer());
        assertTrue(stored.expectsResponse());
        assertEquals("test.Channel", stored.channelType());
        assertEquals("proxy-2", drained.getFirst().from());
        assertEquals(now.get(), drained.getFirst().queuedAt());
    }

    @Test
    void drainMatchesServerIdCaseInsensitivelyAndKeepsOthers() {
        queue.queue(ALEX, SURVIVAL, message("a"), "proxy");
        queue.queue(ALEX, LOBBY, message("b"), "proxy");
        queue.queue(ALEX, SURVIVAL, message("c"), "proxy");

        var drained = queue.drain("SURVIVAL-1", ALEX);

        assertEquals(List.of("a", "c"), drained.stream().map(PluginMessageQueueTest::text).toList());
        assertTrue(queue.drain("survival-1", ALEX).isEmpty());
        assertEquals(1, queue.drain("lobby", ALEX).size());
    }

    @Test
    void drainIsPerPlayer() {
        queue.queue(ALEX, SURVIVAL, message("a"), "proxy");

        assertTrue(queue.drain("survival-1", STEVE).isEmpty());
        assertEquals(1, queue.drain("survival-1", ALEX).size());
    }

    @Test
    void messagesExpireAfterFiveMinutes() {
        queue.queue(ALEX, SURVIVAL, message("old"), "proxy");
        now.addAndGet(TTL);
        queue.queue(ALEX, SURVIVAL, message("fresh"), "proxy");
        now.addAndGet(1);

        var drained = queue.drain("survival-1", ALEX);

        assertEquals(List.of("fresh"), drained.stream().map(PluginMessageQueueTest::text).toList());
    }

    @Test
    void messageAtExactlyTtlIsStillDelivered() {
        queue.queue(ALEX, SURVIVAL, message("edge"), "proxy");
        now.addAndGet(TTL);

        assertEquals(1, queue.drain("survival-1", ALEX).size());
    }

    @Test
    void queueStopsAcceptingAtOneThousandMessages() {
        for (int i = 0; i < 1001; i++) {
            queue.queue(ALEX, SURVIVAL, message("m" + i), "proxy");
        }

        var drained = queue.drain("survival-1", ALEX);

        assertEquals(1000, drained.size());
        assertEquals("m999", text(drained.getLast()));
    }

    @Test
    void drainingFreesCapacity() {
        for (int i = 0; i < 1000; i++) {
            queue.queue(ALEX, SURVIVAL, message("m" + i), "proxy");
        }
        queue.drain("survival-1", ALEX);

        queue.queue(STEVE, SURVIVAL, message("after"), "proxy");

        assertEquals(1, queue.drain("survival-1", STEVE).size());
    }

    @Test
    void expiredMessagesFreeCapacity() {
        for (int i = 0; i < 1000; i++) {
            queue.queue(ALEX, SURVIVAL, message("m" + i), "proxy");
        }
        now.addAndGet(TTL + 1);

        queue.queue(STEVE, SURVIVAL, message("after"), "proxy");

        assertEquals(1, queue.drain("survival-1", STEVE).size(),
                "expired messages of a player who never returns should not keep the queue full");
    }

    @Test
    void removeByServerDropsMatchingMessagesForAllPlayers() {
        queue.queue(ALEX, SURVIVAL, message("a"), "proxy");
        queue.queue(STEVE, SURVIVAL, message("b"), "proxy");
        queue.queue(STEVE, LOBBY, message("c"), "proxy");

        queue.removeByServer("survival-1");

        assertTrue(queue.drain("survival-1", ALEX).isEmpty());
        assertTrue(queue.drain("survival-1", STEVE).isEmpty());
        assertEquals(1, queue.drain("lobby", STEVE).size());
    }

    @Test
    void removeByServerFreesCapacity() {
        for (int i = 0; i < 1000; i++) {
            queue.queue(ALEX, SURVIVAL, message("m" + i), "proxy");
        }
        queue.removeByServer("SURVIVAL-1");

        queue.queue(STEVE, LOBBY, message("after"), "proxy");

        assertEquals(1, queue.drain("lobby", STEVE).size());
    }
}
