package dev.objz.commandbridge.api.message;

/** A handle to a registered listener that can be cancelled. */
@FunctionalInterface
public interface Subscription {
    /** Stops the listener from receiving further events. */
    void cancel();
}
