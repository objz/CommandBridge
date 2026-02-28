package dev.objz.commandbridge.velocity.util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTracker {

    private final ConcurrentHashMap<String, Set<UUID>> playersByClient = new ConcurrentHashMap<>();

    public void update(String clientId, Set<UUID> players) {
        if (clientId == null) return;
        Set<UUID> set = playersByClient.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet());
        set.clear();
        set.addAll(players);
    }

    public void addPlayer(String clientId, UUID playerUuid) {
        if (clientId == null || playerUuid == null) return;
        playersByClient.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(playerUuid);
    }

    public void removePlayer(String clientId, UUID playerUuid) {
        if (clientId == null || playerUuid == null) return;
        Set<UUID> players = playersByClient.get(clientId);
        if (players != null) {
            players.remove(playerUuid);
        }
    }

    public void remove(String clientId) {
        if (clientId == null) return;
        playersByClient.remove(clientId);
    }

    public boolean isPlayerOn(UUID playerUuid, String clientId) {
        if (playerUuid == null || clientId == null) return false;
        Set<UUID> players = playersByClient.get(clientId);
        return players != null && players.contains(playerUuid);
    }
}
