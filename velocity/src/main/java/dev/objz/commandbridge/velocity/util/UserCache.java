package dev.objz.commandbridge.velocity.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.util.MojangAPI;
import dev.objz.commandbridge.velocity.net.out.ctx.ResolveUuidRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class UserCache {

    private static final Duration REMOTE_TIMEOUT = Duration.ofSeconds(5);

    private final ProxyServer proxy;
    private final SessionHub sessions;
    private final OutNode outNode;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Path cachePath;

    public UserCache(ProxyServer proxy, SessionHub sessions, OutNode outNode, Path cachePath) {
        this.proxy = Objects.requireNonNull(proxy);
        this.sessions = Objects.requireNonNull(sessions);
        this.outNode = Objects.requireNonNull(outNode);
        this.cachePath = Objects.requireNonNull(cachePath);
        load();
    }

    public void cachePlayer(String name, UUID uuid) {
        if (name == null || uuid == null) return;
        cache.put(name.toLowerCase(Locale.ROOT), new CacheEntry(name, uuid.toString()));
    }

    public Collection<String> knownNames() {
        return cache.values().stream()
                .map(CacheEntry::name)
                .toList();
    }

    public CompletableFuture<UUID> resolve(String name) {
        if (name == null || name.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        // 1. Local proxy — online on this Velocity instance
        Optional<Player> local = proxy.getPlayer(name);
        if (local.isPresent()) {
            UUID uuid = local.get().getUniqueId();
            cachePlayer(name, uuid);
            return CompletableFuture.completedFuture(uuid);
        }

        // 2. Remote proxies — query all connected Velocity-type clients
        return queryRemoteProxies(name).thenCompose(remoteResult -> {
            if (remoteResult != null) {
                cachePlayer(name, remoteResult);
                return CompletableFuture.completedFuture(remoteResult);
            }

            // 3. usercache.json
            CacheEntry cached = cache.get(name.toLowerCase(Locale.ROOT));
            if (cached != null) {
                try {
                    UUID uuid = UUID.fromString(cached.uuid());
                    return CompletableFuture.completedFuture(uuid);
                } catch (IllegalArgumentException ignored) {
                }
            }

            // 4. Mojang HTTP API
            return CompletableFuture.supplyAsync(() -> {
                UUID mojangUuid = MojangAPI.getUuid(name);
                if (mojangUuid != null) {
                    cachePlayer(name, mojangUuid);
                    save();
                }
                return mojangUuid;
            });
        });
    }

    private CompletableFuture<UUID> queryRemoteProxies(String name) {
        List<ClientSession> velocitySessions = new ArrayList<>();
        for (ClientSession session : sessions) {
            if (session.location() == Location.VELOCITY
                    && session.status() == AuthStatus.AUTH_OK
                    && session.endpoint() != null
                    && session.endpoint().isOpen()) {
                velocitySessions.add(session);
            }
        }

        if (velocitySessions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<UUID> combinedResult = new CompletableFuture<>();
        List<CompletableFuture<UUID>> futures = new ArrayList<>(velocitySessions.size());

        for (ClientSession session : velocitySessions) {
            CompletableFuture<UUID> future = new CompletableFuture<>();
            futures.add(future);

            var ctx = new ResolveUuidRequestContext(session, name, REMOTE_TIMEOUT, future);
            try {
                outNode.send(MessageType.RESOLVE_UUID, ctx);
            } catch (Exception e) {
                Log.debug("Failed to send RESOLVE_UUID to '{}': {}", session.id(), e.getMessage());
                future.complete(null);
            }
        }

        // Complete with the first non-null result
        for (CompletableFuture<UUID> future : futures) {
            future.thenAccept(uuid -> {
                if (uuid != null) {
                    combinedResult.complete(uuid);
                }
            });
        }

        // When all complete, if still not resolved, complete with null
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((v, ex) -> combinedResult.complete(null));

        return combinedResult;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(cachePath.getParent());
            List<CacheEntry> entries = new ArrayList<>(cache.values());
            mapper.writeValue(cachePath.toFile(), entries);
        } catch (IOException e) {
            Log.error("Failed to save user cache: {}", e.getMessage());
        }
    }

    private synchronized void load() {
        if (Files.notExists(cachePath)) return;
        try {
            List<CacheEntry> entries = mapper.readValue(cachePath.toFile(),
                    new TypeReference<List<CacheEntry>>() {
                    });
            for (CacheEntry entry : entries) {
                if (entry.name() != null && entry.uuid() != null) {
                    cache.put(entry.name().toLowerCase(Locale.ROOT), entry);
                }
            }
            Log.success(true, "Loaded '{}' cached user entries", cache.size());
        } catch (IOException e) {
            Log.error("Failed to load user cache: {}", e.getMessage());
        }
    }

    private record CacheEntry(String name, String uuid) {
    }
}
