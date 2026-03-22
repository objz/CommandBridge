package dev.objz.commandbridge.velocity.util;

import dev.objz.commandbridge.scripting.model.enums.Location;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class PlayerTracker {

    private final ConcurrentHashMap<String, Set<UUID>> playersByClient = new ConcurrentHashMap<>();
    private final List<BiConsumer<String, UUID>> joinListeners = new CopyOnWriteArrayList<>();

    public void onPlayerJoin(BiConsumer<String, UUID> listener) {
        joinListeners.add(listener);
    }

    public void update(String clientId, Set<UUID> players) {
        if (clientId == null)
            return;
        Set<UUID> set = playersByClient.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet());
        Set<UUID> previousPlayers = Set.copyOf(set);
        set.clear();
        set.addAll(players);
        for (UUID uuid : players) {
            if (!previousPlayers.contains(uuid)) {
                fireJoin(clientId, uuid);
            }
        }
    }

    public void addPlayer(String clientId, UUID playerUuid) {
        if (clientId == null || playerUuid == null)
            return;
        playersByClient.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(playerUuid);
        fireJoin(clientId, playerUuid);
    }

    public void removePlayer(String clientId, UUID playerUuid) {
        if (clientId == null || playerUuid == null)
            return;
        Set<UUID> players = playersByClient.get(clientId);
        if (players != null) {
            players.remove(playerUuid);
        }
    }

    public void remove(String clientId) {
        if (clientId == null)
            return;
        playersByClient.remove(clientId);
    }

    public boolean isPlayerOn(UUID playerUuid, String clientId) {
        if (playerUuid == null || clientId == null)
            return false;
        Set<UUID> players = playersByClient.get(clientId);
        return players != null && players.contains(playerUuid);
    }

    public boolean isPlayerOnTarget(UUID playerUuid, String targetId,
            Location targetLocation, String localVelocityId) {
        return switch (targetLocation) {
            case VELOCITY -> targetId.equals(localVelocityId)
                    || isPlayerOn(playerUuid, targetId);
            case BACKEND -> isPlayerOn(playerUuid, targetId);
        };
    }

    private void fireJoin(String clientId, UUID playerUuid) {
        for (var listener : joinListeners) {
            try {
                listener.accept(clientId, playerUuid);
            } catch (Exception ignore) {
            }
        }
    }
}
