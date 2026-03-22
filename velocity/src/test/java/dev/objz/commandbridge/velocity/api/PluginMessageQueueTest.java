package dev.objz.commandbridge.velocity.api;

import static dev.objz.commandbridge.api.platform.Platform.backend;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.PluginMessage;

class PluginMessageQueueTest {

    private PluginMessageQueue queue;

    @BeforeAll
    static void installLog() {
        try {
            Log.install(java.util.logging.Logger.getLogger("test"));
        } catch (IllegalStateException ignored) { }
    }

    @BeforeEach
    void setUp() {
        queue = new PluginMessageQueue();
    }

    @Test
    void queueAndDrain() {
        UUID player = UUID.randomUUID();
        Platform.ServerTarget target = backend("survival-1");
        PluginMessage msg = new PluginMessage("test-channel", null, false);

        queue.queue(player, target, msg, "velocity");

        List<PluginMessageQueue.QueuedMessage> drained = queue.drain("survival-1", player);
        assertEquals(1, drained.size());
        assertEquals("survival-1", drained.get(0).target().id());

        List<PluginMessageQueue.QueuedMessage> second = queue.drain("survival-1", player);
        assertTrue(second.isEmpty());
    }

    @Test
    void fifoOrdering() {
        UUID player = UUID.randomUUID();
        Platform.ServerTarget target = backend("s1");

        queue.queue(player, target, new PluginMessage("msgA", null, false), "velocity");
        queue.queue(player, target, new PluginMessage("msgB", null, false), "velocity");
        queue.queue(player, target, new PluginMessage("msgC", null, false), "velocity");

        List<PluginMessageQueue.QueuedMessage> drained = queue.drain("s1", player);
        assertEquals(3, drained.size());
        assertEquals("msgA", drained.get(0).message().channelType());
        assertEquals("msgB", drained.get(1).message().channelType());
        assertEquals("msgC", drained.get(2).message().channelType());
    }

    @Test
    void serverDisconnectCleanup() {
        UUID player = UUID.randomUUID();
        Platform.ServerTarget target = backend("survival-1");
        PluginMessage msg = new PluginMessage("test-channel", null, false);

        queue.queue(player, target, msg, "velocity");
        queue.removeByServer("survival-1");

        List<PluginMessageQueue.QueuedMessage> drained = queue.drain("survival-1", player);
        assertTrue(drained.isEmpty());
    }

    @Test
    void whenOnlineFieldStripped() {
        UUID player = UUID.randomUUID();
        UUID onlineTarget = UUID.randomUUID();
        Platform.ServerTarget target = backend("s1");
        PluginMessage msg = new PluginMessage("test-channel", null, false, null, onlineTarget, null);

        queue.queue(player, target, msg, "velocity");

        List<PluginMessageQueue.QueuedMessage> drained = queue.drain("s1", player);
        assertEquals(1, drained.size());
        assertNull(drained.get(0).message().whenOnline());
    }

    @Test
    void queueCapEnforced() {
        UUID player = UUID.randomUUID();
        Platform.ServerTarget target = backend("s1");

        for (int i = 0; i < 1001; i++) {
            queue.queue(player, target, new PluginMessage("msg-" + i, null, false), "velocity");
        }

        List<PluginMessageQueue.QueuedMessage> drained = queue.drain("s1", player);
        assertEquals(1000, drained.size());
    }
}
