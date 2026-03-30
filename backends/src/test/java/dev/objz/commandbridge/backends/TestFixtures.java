package dev.objz.commandbridge.backends;

import dev.objz.commandbridge.logging.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared test utilities for the backends module.
 * Provides Log singleton initialization to prevent IllegalStateException across tests.
 */
public final class TestFixtures {

    private static final AtomicBoolean LOG_INSTALLED = new AtomicBoolean();

    public static void ensureLog() {
        if (LOG_INSTALLED.compareAndSet(false, true)) {
            Log.install(java.util.logging.Logger.getLogger("test"));
        }
    }

    private TestFixtures() {
    }
}
