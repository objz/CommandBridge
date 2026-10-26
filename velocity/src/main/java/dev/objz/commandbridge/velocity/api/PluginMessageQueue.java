package dev.objz.commandbridge.velocity.api;

import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.PluginMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public final class PluginMessageQueue {

    private static final long TTL_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_TOTAL = 1000;

    public record QueuedMessage(Platform.ServerTarget target, PluginMessage message, String from, long queuedAt) {
    }

    private final ConcurrentHashMap<UUID, Queue<QueuedMessage>> queues = new ConcurrentHashMap<>();
    private final AtomicInteger totalSize = new AtomicInteger(0);
    private final LongSupplier clock;

    public PluginMessageQueue() {
        this(System::currentTimeMillis);
    }

    PluginMessageQueue(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public void queue(UUID playerUuid, Platform.ServerTarget target, PluginMessage message, String from) {
        Objects.requireNonNull(playerUuid);
        Objects.requireNonNull(target);
        Objects.requireNonNull(message);
        // expired messages of players who never came back would otherwise keep the queue full for good
        if (totalSize.get() >= MAX_TOTAL)
            pruneExpired(clock.getAsLong());
        if (totalSize.get() >= MAX_TOTAL) {
            Log.warn("PluginMessageQueue is full ({} entries). Dropping message for player {}", MAX_TOTAL, playerUuid);
            return;
        }
        // Strip whenOnline from stored message to prevent re-queue on replay
        PluginMessage stored = new PluginMessage(message.channelType(), message.data(),
                message.expectsResponse(), message.requirePlayer(), null, message.error());
        queues.computeIfAbsent(playerUuid, k -> new ConcurrentLinkedQueue<>())
                .add(new QueuedMessage(target, stored, from, clock.getAsLong()));
        totalSize.incrementAndGet();
    }

    public List<QueuedMessage> drain(String clientId, UUID playerUuid) {
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(playerUuid);
        Queue<QueuedMessage> queue = queues.get(playerUuid);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        long now = clock.getAsLong();
        List<QueuedMessage> result = new ArrayList<>();
        Iterator<QueuedMessage> it = queue.iterator();
        while (it.hasNext()) {
            QueuedMessage entry = it.next();
            if (now - entry.queuedAt() > TTL_MS) {
                it.remove();
                totalSize.decrementAndGet();
                continue;
            }
            if (entry.target().id().equalsIgnoreCase(clientId)) {
                it.remove();
                totalSize.decrementAndGet();
                result.add(entry);
            }
        }
        if (queue.isEmpty()) {
            queues.remove(playerUuid, queue);
        }
        return List.copyOf(result);
    }

    private void pruneExpired(long now) {
        for (Map.Entry<UUID, Queue<QueuedMessage>> entry : queues.entrySet()) {
            Queue<QueuedMessage> queue = entry.getValue();
            if (queue.removeIf(m -> now - m.queuedAt() > TTL_MS)) {
                totalSize.set(queues.values().stream().mapToInt(Queue::size).sum());
            }
            if (queue.isEmpty()) {
                queues.remove(entry.getKey(), queue);
            }
        }
    }

    public void removeByServer(String serverId) {
        Objects.requireNonNull(serverId);
        for (Map.Entry<UUID, Queue<QueuedMessage>> entry : queues.entrySet()) {
            Queue<QueuedMessage> queue = entry.getValue();
            Iterator<QueuedMessage> it = queue.iterator();
            while (it.hasNext()) {
                if (it.next().target().id().equalsIgnoreCase(serverId)) {
                    it.remove();
                    totalSize.decrementAndGet();
                }
            }
            if (queue.isEmpty()) {
                queues.remove(entry.getKey(), queue);
            }
        }
    }
}
