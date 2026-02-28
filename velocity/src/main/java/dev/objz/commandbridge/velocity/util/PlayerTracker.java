package dev.objz.commandbridge.velocity.util;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTracker {

    private final ConcurrentHashMap<String, Set<UUID>> playersByClient = new ConcurrentHashMap<>();

    public void update(String clientId, Set<UUID> players) {
        if (clientId == null) return;
        playersByClient.put(clientId, Collections.unmodifiableSet(players));
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
