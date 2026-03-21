package dev.objz.commandbridge.api.message;

@FunctionalInterface
public interface Subscription {
    void cancel();
}
