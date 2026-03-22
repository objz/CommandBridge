package dev.objz.commandbridge.net;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import dev.objz.commandbridge.net.proto.Envelope;

public final class ResponseAwaiter {

    private record Key(String clientId, UUID id) {
    }

    private record Pending(Predicate<Envelope> match, CompletableFuture<Envelope> fut) {
    }

    private final ConcurrentHashMap<Key, Pending> waiters = new ConcurrentHashMap<>();

    public CompletableFuture<Envelope> await(String clientId,
            UUID id,
            Predicate<Envelope> matcher,
            Duration timeout) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(matcher);
        var fut = new CompletableFuture<Envelope>();
        var key = new Key(clientId, id);
        waiters.put(key, new Pending(matcher, fut));
        fut.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((ok, ex) -> waiters.remove(key));
        return fut;
    }

    public boolean signal(Envelope env) {
        if (env == null || env.id() == null)
            return false;

        boolean handled = false;
        UUID id = env.id();

        var exactKey = new Key(env.from(), id);
        var pending = waiters.get(exactKey);
        if (pending != null && pending.match().test(env)) {
            pending.fut().complete(env);
            waiters.remove(exactKey);
            handled = true;
        } else {
            var anyClientKey = new Key(null, id);
            pending = waiters.get(anyClientKey);
            if (pending != null && pending.match().test(env)) {
                pending.fut().complete(env);
                waiters.remove(anyClientKey);
                handled = true;
            }
        }
        return handled;
    }
}
