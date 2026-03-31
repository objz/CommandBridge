package dev.objz.commandbridge.api;

import java.util.Objects;

/**
 * Static provider for obtaining the {@link CommandBridgeAPI} singleton
 * instance.
 *
 * <p>
 * CommandBridge registers its implementation when the plugin enables.
 * Third-party plugins
 * retrieve the instance by calling {@link #get()} or {@link #get(Class)} for
 * platform-specific
 * subtypes. The provider holds a single volatile reference accessible across
 * threads.
 *
 * <p>
 * This class cannot be instantiated. All access is through static methods.
 *
 * <p>
 * To obtain the API instance:
 *
 * <pre>{@code
 * CommandBridgeAPI api = CommandBridgeProvider.get();
 * }</pre>
 *
 * <p>
 * To access a platform-specific subtype, pass the expected class to
 * {@link #get(Class)}.
 *
 * @see CommandBridgeAPI
 */
public final class CommandBridgeProvider {

    private static volatile CommandBridgeAPI instance;

    private CommandBridgeProvider() {
        throw new UnsupportedOperationException("Provider can't be instanced");
    }

    /**
     * Returns the registered {@link CommandBridgeAPI} instance.
     *
     * <p>
     * This method is safe to call from any thread after CommandBridge has enabled.
     *
     * @return the registered {@code CommandBridgeAPI} instance; never {@code null}
     * @throws IllegalStateException if CommandBridge is not installed, not yet
     *                               enabled,
     *                               or the API has not been registered
     * @see #get(Class)
     */
    public static CommandBridgeAPI get() {
        if (instance == null) {
            throw new IllegalStateException("CommandBridge is not available. Is it installed and running?");
        }
        return instance;
    }

    /**
     * Returns the registered {@link CommandBridgeAPI} instance cast to the
     * specified subtype.
     *
     * <p>
     * Use this method when accessing a platform-specific extension of the API. The
     * cast is
     * validated at runtime; if the registered instance does not implement the
     * requested type,
     * an {@link IllegalStateException} is thrown.
     *
     * @param <T>  the expected API subtype; must extend {@link CommandBridgeAPI}
     * @param type the {@link Class} token of the expected subtype; must not be
     *             {@code null}
     * @return the API instance cast to {@code T}; never {@code null}
     * @throws IllegalStateException if the API is not registered, or if the
     *                               registered instance
     *                               is not an instance of {@code type}
     * @see #get()
     */
    public static <T extends CommandBridgeAPI> T get(Class<T> type) {
        CommandBridgeAPI api = get();
        if (!type.isInstance(api)) {
            throw new IllegalStateException(type.getSimpleName() + " is not available on this platform");
        }
        return type.cast(api);
    }

    /**
     * Registers the {@link CommandBridgeAPI} implementation.
     *
     * <p>
     * This method is called internally by the CommandBridge plugin during startup
     * and is not
     * intended for use by third-party plugins.
     *
     * @param impl the implementation to register; must not be {@code null}
     * @throws IllegalStateException if an implementation is already registered
     * @throws NullPointerException  if {@code impl} is {@code null}
     */
    public static synchronized void register(CommandBridgeAPI impl) {
        Objects.requireNonNull(impl);
        if (instance != null) {
            throw new IllegalStateException("CommandBridge already registered");
        }
        instance = impl;
    }

    /**
     * Clears the registered {@link CommandBridgeAPI} implementation.
     *
     * <p>
     * Called internally by the CommandBridge plugin during shutdown. After this
     * method returns,
     * calls to {@link #get()} will throw {@link IllegalStateException} until a new
     * implementation
     * is registered.
     */
    public static synchronized void unregister() {
        instance = null;
    }
}
