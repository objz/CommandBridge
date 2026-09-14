package dev.objz.commandbridge.net.redis;

public final class RedisChannels {
    public static final String PROXY_INBOUND = "commandbridge:proxy:in";
    public static final String CLIENT_CONTROL = "commandbridge:clients:control";
    private static final String CLIENT_PREFIX = "commandbridge:client:";

    private RedisChannels() {
    }

    public static String clientInbound(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return CLIENT_PREFIX + "unknown";
        }
        return CLIENT_PREFIX + clientId.trim();
    }
}
