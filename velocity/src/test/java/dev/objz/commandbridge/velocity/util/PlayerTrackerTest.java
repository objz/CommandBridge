package dev.objz.commandbridge.velocity.util;

import dev.objz.commandbridge.velocity.TestFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PlayerTracker} — verifies player add/remove, update with join
 * listener firing, unknown player/client lookups, and listener exception isolation.
 */
final class PlayerTrackerTest {

    private PlayerTracker tracker;

    @BeforeAll
    static void initLog() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setUp() {
        tracker = new PlayerTracker();
    }

    @Test
    void addPlayerStoresMapping() {
        UUID uuid = UUID.randomUUID();
        tracker.addPlayer("client1", uuid);

        assertTrue(tracker.isPlayerOn(uuid, "client1"));
    }

    @Test
    void removePlayerRemovesMapping() {
        UUID uuid = UUID.randomUUID();
        tracker.addPlayer("client1", uuid);
        tracker.removePlayer("client1", uuid);

        assertFalse(tracker.isPlayerOn(uuid, "client1"));
    }

    @Test
    void isPlayerOnUnknownPlayerReturnsFalse() {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        tracker.addPlayer("client1", known);

        assertFalse(tracker.isPlayerOn(unknown, "client1"));
    }

    @Test
    void isPlayerOnUnknownClientReturnsFalse() {
        UUID uuid = UUID.randomUUID();
        tracker.addPlayer("client1", uuid);

        assertFalse(tracker.isPlayerOn(uuid, "unknown-client"));
    }

    @Test
    void updateAddsNewPlayers() {
        UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        tracker.update("c1", Set.of(uuid1, uuid2));

        assertTrue(tracker.isPlayerOn(uuid1, "c1"));
        assertTrue(tracker.isPlayerOn(uuid2, "c1"));
    }

    @Test
    void updateFiresJoinListenersForNewPlayers() {
        UUID uuid = UUID.randomUUID();
        List<String> captured = new ArrayList<>();

        tracker.onPlayerJoin((clientId, playerUuid) ->
                captured.add(clientId + ":" + playerUuid));

        tracker.update("server1", Set.of(uuid));

        assertEquals(1, captured.size());
        assertEquals("server1:" + uuid, captured.get(0));
    }

    @Test
    void joinListenerExceptionDoesNotBreakOtherListeners() {
        UUID uuid = UUID.randomUUID();
        List<UUID> secondListenerCaptures = new ArrayList<>();

        BiConsumer<String, UUID> throwingListener = (clientId, playerUuid) -> {
            throw new RuntimeException("boom");
        };
        BiConsumer<String, UUID> capturingListener = (clientId, playerUuid) ->
                secondListenerCaptures.add(playerUuid);

        tracker.onPlayerJoin(throwingListener);
        tracker.onPlayerJoin(capturingListener);

        tracker.addPlayer("client1", uuid);

        assertEquals(1, secondListenerCaptures.size());
        assertEquals(uuid, secondListenerCaptures.get(0));
    }
}
