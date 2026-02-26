package dev.objz.commandbridge.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record BackendsConfig(
        @Setting("client-id") String clientId,
        @Setting("endpoint-type") EndpointType endpointType,
        @Setting("endpoints") Endpoints endpoints,
        @Setting("security") Security security,
        @Setting("timeouts") Timeouts timeouts,
        @Setting("debug") boolean debug) {
    @ConfigSerializable
    public static record Endpoints(
            @Setting("websocket") WebSocket websocket,
            @Setting("redis") Redis redis) {

        @ConfigSerializable
        public static record WebSocket(
                @Setting("host") String host,
                @Setting("port") int port) {
        }

        @ConfigSerializable
        public static record Redis(
                @Setting("host") String host,
                @Setting("port") int port,
                @Setting("username") String username,
                @Setting("password") String password) {
        }
    }

    @ConfigSerializable
    public static record Security(
            @Setting("tls-mode") TlsMode tlsMode,
            @Setting("tls-pin") String tlsPin,
            @Setting("secret") String secret) {
    }

    @ConfigSerializable
    public static record Timeouts(
            @Setting("auth-timeout") int authTimeout,
            @Setting("reconnect-timeout") int reconnectTimeout,
            @Setting("reconnect-interval") int reconnectInterval
    ) {
    }


    public static BackendsConfig defaults() {
        return new BackendsConfig(
                "survival-1",
                EndpointType.WEBSOCKET,
                new Endpoints(
                        new Endpoints.WebSocket("127.0.0.1", 8765),
                        new Endpoints.Redis("127.0.0.1", 6379, "", "")),
                new Security(TlsMode.TOFU, "", "change-me"),
                new Timeouts(5, 60, 5),
                false);
    }
}
