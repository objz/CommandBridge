package dev.objz.commandbridge.api;

import java.util.Objects;

/** Static provider for accessing the {@link CommandBridgeAPI} instance. */
public final class CommandBridgeProvider {

    private static volatile CommandBridgeAPI instance;

    private CommandBridgeProvider() {
        throw new UnsupportedOperationException("Provider can't be instanced");
    }

    /**
     * @return the registered API instance
     * @throws IllegalStateException if the API is not registered
     */
    public static CommandBridgeAPI get() {
        if (instance == null) {
            throw new IllegalStateException("CommandBridge is not available. Is it installed and running?");
        }
        return instance;
    }

    /**
     * Obtains the API instance cast to a specific type.
     *
     * @param type the API class type
     * @param <T> the API type
     * @return the cast API instance
     * @throws IllegalStateException if the instance is not available or compatible
     */
    public static <T extends CommandBridgeAPI> T get(Class<T> type) {
        CommandBridgeAPI api = get();
        if (!type.isInstance(api)) {
            throw new IllegalStateException(type.getSimpleName() + " is not available on this platform");
        }
        return type.cast(api);
    }

    /**
     * Registers the API implementation.
     *
     * @param impl the implementation to register
     * @throws IllegalStateException if an implementation is already registered
     */
    public static synchronized void register(CommandBridgeAPI impl) {
        Objects.requireNonNull(impl);
        if (instance != null) {
            throw new IllegalStateException("CommandBridge already registered");
        }
        instance = impl;
    }

    /** Unregisters the current API implementation. */
    public static synchronized void unregister() {
        instance = null;
    }
}
