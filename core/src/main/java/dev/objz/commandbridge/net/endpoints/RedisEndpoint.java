package dev.objz.commandbridge.net.endpoints;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.proto.Envelope;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class RedisEndpoint implements Endpoint {
    private final String id;
    private final Function<Envelope, CompletableFuture<Void>> sender;
    private final BooleanSupplier openSupplier;

    public RedisEndpoint(String id,
            Function<Envelope, CompletableFuture<Void>> sender,
            BooleanSupplier openSupplier) {
        this.id = Objects.requireNonNull(id);
        this.sender = Objects.requireNonNull(sender);
        this.openSupplier = Objects.requireNonNull(openSupplier);
    }

    public String id() {
        return id;
    }

    @Override
    public CompletableFuture<Void> send(Envelope env) {
        return sender.apply(env);
    }

    @Override
    public boolean isOpen() {
        return openSupplier.getAsBoolean();
    }

    @Override
    public String describe() {
        return "redis:" + id;
    }
}
