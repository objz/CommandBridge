package dev.objz.commandbridge.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record VelocityConfig(
        @Setting("act-as-client") boolean actAsClient,
        @Setting("server-id") String serverId,
        @Setting("endpoint-type") EndpointType endpointType,
        @Setting("endpoints") Endpoints endpoints,
        @Setting("heartbeat") Heartbeat heartbeat,
        @Setting("security") Security security,
        @Setting("timeouts") Timeouts timeouts,
        @Setting("limits") Limits limits,
        @Setting("debug") boolean debug) {

    @ConfigSerializable
    public static record Endpoints(
            @Setting("websocket") WebSocket webSocket,
            @Setting("redis") Redis redis

    ) {
        @ConfigSerializable
        public static record WebSocket(
                @Setting("bind-host") String bindHost,
                @Setting("bind-port") int bindPort) {
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
    public static record Heartbeat(
            @Setting("app-ping-seconds") int appPingSeconds,
            @Setting("stale-after-seconds") int staleAfterSeconds) {
    }

    @ConfigSerializable
    public static record Security(
            @Setting("require-auth") boolean requireAuth,
            @Setting("auth-timeout-seconds") int authTimeoutSeconds,
            @Setting("tls-mode") TlsMode tlsMode,
            @Setting("keystore-path") String keystorePath,
            @Setting("keystore-password") String keystorePassword,
            @Setting("keystore-type") String keystoreType // PKCS12 | JKS
    ) {
    }

    @ConfigSerializable
    public static record Timeouts(
            @Setting("register-timeout") int registerTimeout,
            @Setting("ping-timeout") int pingTimeout) {
    }

    @ConfigSerializable
    public static record Limits(
            @Setting("inbound-messages-per-sec") int inboundMessagesSec,
            @Setting("max-connections") int maxConnections,
            @Setting("max-message-size-bytes") int maxMessageSizeBytes) {
    }

    public static VelocityConfig defaults() {
        return new VelocityConfig(
                false,
                "proxy-1",
                EndpointType.WEBSOCKET,
                new Endpoints(
                        new Endpoints.WebSocket("0.0.0.0", 8765),
                        new Endpoints.Redis("127.0.0.1", 6379, "", "")),
                new Heartbeat(10, 60),
                new Security(true, 10, TlsMode.TOFU, "", "", "PKCS12"),
                new Timeouts(5, 5),
                new Limits(60, 100, 65_536),
                false);
    }
}
