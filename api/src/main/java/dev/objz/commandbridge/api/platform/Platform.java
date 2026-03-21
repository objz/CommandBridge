package dev.objz.commandbridge.api.platform;

/** Enumeration of supported bridge platforms. */
public enum Platform {
    /** A backend Minecraft server (e.g. Paper, Folia). */
    BACKEND,
    /** The Velocity proxy. */
    VELOCITY;

    /**
     * Creates a {@link ServerTarget} for this platform.
     *
     * @param id the unique server identifier
     * @return the target representation
     */
    public ServerTarget target(String id) {
        return new ServerTarget(id, this);
    }

    /**
     * Identifies a specific server in the bridge network.
     *
     * @param id the unique server identifier
     * @param type the platform type of the server
     */
    public record ServerTarget(String id, Platform type) {
    }
}
