package dev.objz.commandbridge.api.platform;

public enum Platform {
    BACKEND,
    VELOCITY;

    public ServerTarget target(String id) {
        return new ServerTarget(id, this);
    }

    public record ServerTarget(String id, Platform type) {
    }
}
