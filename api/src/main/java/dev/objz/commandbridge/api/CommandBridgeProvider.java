package dev.objz.commandbridge.api;

import java.util.Objects;

public final class CommandBridgeProvider {

    private static volatile CommandBridgeAPI instance;

    private CommandBridgeProvider() {
        throw new UnsupportedOperationException("Provider can't be instanced");
    }

    public static CommandBridgeAPI get() {
        if (instance == null) {
            throw new IllegalStateException("CommandBridge is not available. Is it installed and running?");
        }
        return instance;
    }

    public static <T extends CommandBridgeAPI> T get(Class<T> type) {
        CommandBridgeAPI api = get();
        if (!type.isInstance(api)) {
            throw new IllegalStateException(type.getSimpleName() + " is not available on this platform");
        }
        return type.cast(api);
    }

    static void register(CommandBridgeAPI impl) {
        Objects.requireNonNull(impl);
        if (instance != null) {
            throw new IllegalStateException("CommandBridge already registered");
        }
        instance = impl;
    }

    static void unregister() {
        instance = null;
    }
}
