package dev.objz.commandbridge.api.platform;

/**
 * Enumeration of the two supported bridge platforms: the Velocity proxy and
 * backend servers.
 *
 * <p>
 * Each constant identifies a category of server in the CommandBridge network.
 * Use the
 * factory methods {@link #backend(String)} and {@link #velocity(String)} to
 * construct typed
 * {@link ServerTarget} instances that can be passed to a message channel's
 * target builder.
 *
 * <pre>{@code
 * // Target a specific backend server
 * Platform.ServerTarget survival = Platform.backend("survival-1");
 *
 * // Target the Velocity proxy itself (multi-proxy setup)
 * Platform.ServerTarget proxy = Platform.velocity("proxy-1");
 * }</pre>
 *
 * @see Platform.ServerTarget
 */
public enum Platform {

    /**
     * Represents a backend Minecraft server (for example, Paper, Folia, or Bukkit).
     *
     * <p>
     * Use this platform when targeting backend servers to send messages via a
     * channel.
     */
    BACKEND,

    /**
     * Represents the Velocity proxy.
     *
     * <p>
     * Use this platform when targeting the proxy itself, for example in a
     * multi-proxy
     * setup where one Velocity instance acts as a client to another.
     */
    VELOCITY;

    /**
     * Creates a {@link ServerTarget} for this platform with the given identifier.
     *
     * @param id the unique server identifier
     * @return a new {@code ServerTarget} bound to this platform
     * @see #backend(String)
     * @see #velocity(String)
     */
    public ServerTarget target(String id) {
        return new ServerTarget(id, this);
    }

    /**
     * Creates a {@link ServerTarget} identifying a backend server.
     *
     * <p>
     * Equivalent to {@code BACKEND.target(id)}.
     *
     * @param id the unique server identifier
     * @return a {@code ServerTarget} with platform {@link #BACKEND}
     */
    public static ServerTarget backend(String id) {
        return BACKEND.target(id);
    }

    /**
     * Creates a {@link ServerTarget} identifying a Velocity proxy server.
     *
     * <p>
     * Equivalent to {@code VELOCITY.target(id)}.
     *
     * @param id the unique server identifier
     * @return a {@code ServerTarget} with platform {@link #VELOCITY}
     */
    public static ServerTarget velocity(String id) {
        return VELOCITY.target(id);
    }

    /**
     * Identifies a specific server in the bridge network by its unique identifier
     * and platform type.
     *
     * <p>
     * The {@code id} component is the server's unique name as configured in the
     * bridge network.
     * The {@code type} component indicates whether the server is a
     * {@link Platform#BACKEND} or
     * {@link Platform#VELOCITY} instance. Use the factory methods
     * {@link Platform#backend(String)}
     * and {@link Platform#velocity(String)} to construct instances rather than
     * calling this
     * record's constructor directly.
     *
     * @param id   the unique server identifier within the bridge network
     * @param type the platform type of the server
     * @see Platform
     */
    public record ServerTarget(String id, Platform type) {
    }
}
