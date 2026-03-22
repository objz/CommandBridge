package dev.objz.commandbridge.velocity.util;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.model.enums.Location;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTrackerTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UUID_C = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private PlayerTracker tracker;
    private AtomicInteger joinCount;

    @BeforeAll
    static void installLog() {
        try {
            Log.install(Logger.getLogger("PlayerTrackerTest"));
        } catch (IllegalStateException e) {
            // Log already installed, ignore
        }
    }

    @BeforeEach
    void setUp() {
        tracker = new PlayerTracker();
        joinCount = new AtomicInteger(0);
        tracker.onPlayerJoin((clientId, uuid) -> joinCount.incrementAndGet());
    }

    @Test
    void addPlayerFiresJoinListener() {
        tracker.addPlayer("client1", UUID_A);
        assertEquals(1, joinCount.get());
    }

    @Test
    void updateDetectsNewPlayers() {
        tracker.update("client1", Set.of(UUID_A, UUID_B));
        assertEquals(2, joinCount.get());
    }

    @Test
    void updateDoesNotFireForExistingPlayers() {
        tracker.update("client1", Set.of(UUID_A));
        assertEquals(1, joinCount.get());

        tracker.update("client1", Set.of(UUID_A));
        assertEquals(1, joinCount.get());
    }

    @Test
    void removePlayerMakesUntracked() {
        tracker.addPlayer("client1", UUID_A);
        tracker.removePlayer("client1", UUID_A);
        assertFalse(tracker.isPlayerOn(UUID_A, "client1"));
    }

    @Test
    void isPlayerOnReturnsTrue() {
        tracker.addPlayer("client1", UUID_A);
        assertTrue(tracker.isPlayerOn(UUID_A, "client1"));
    }

    @Test
    void isPlayerOnReturnsFalseWhenAbsent() {
        assertFalse(tracker.isPlayerOn(UUID_A, "client1"));
    }

    @Test
    void isPlayerOnTargetVelocityLocalAlwaysTrue() {
        assertTrue(tracker.isPlayerOnTarget(UUID_A, "local-id", Location.VELOCITY, "local-id"));
    }

    @Test
    void isPlayerOnTargetBackend() {
        tracker.addPlayer("server1", UUID_A);
        assertTrue(tracker.isPlayerOnTarget(UUID_A, "server1", Location.BACKEND, "local"));
    }

    @Test
    void nullClientIdIgnored() {
        tracker.addPlayer(null, UUID_A);
        assertEquals(0, joinCount.get());
    }

    @Test
    void nullPlayerUuidIgnored() {
        tracker.addPlayer("client1", null);
        assertEquals(0, joinCount.get());
    }

    @Test
    void listenerExceptionDoesNotCrash() {
        tracker.onPlayerJoin((clientId, uuid) -> {
            throw new RuntimeException("Test exception");
        });
        tracker.addPlayer("client1", UUID_A);
        assertEquals(1, joinCount.get());
    }
}
