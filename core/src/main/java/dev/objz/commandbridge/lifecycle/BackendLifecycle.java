package dev.objz.commandbridge.lifecycle;

/**
 * Lifecycle contract for a backend bootstrap, invoked reflectively from the proxy.
 */
public interface BackendLifecycle {

    void load();

    void enable();

    void disable();
}
