package dev.objz.commandbridge.config.profile;

import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;

public final class VelocityConfigProfile implements ConfigProfile<VelocityConfig> {
    @Override
    public VelocityConfig defaults() {
        return VelocityConfig.defaults();
    }

    @Override
    public Result<VelocityConfig> normalize(VelocityConfig in) {
        VelocityConfig d = defaults();
        boolean ok = true;

        String serverId = in.serverId();
        if (serverId == null || serverId.isBlank()) {
            serverId = d.serverId();
            Log.warn("'server-id' missing or blank. defaulting to '{}'", serverId);
        } else {
            serverId = serverId.trim();
        }

        EndpointType endpointType = in.endpointType();
        if (endpointType == null) {
            Log.error("'endpoint-type' must be set");
            endpointType = d.endpointType();
            ok = false;
        }
        boolean websocketMode = endpointType == EndpointType.WEBSOCKET;
        boolean redisMode = endpointType == EndpointType.REDIS;

        VelocityConfig.Endpoints endpointsIn = in.endpoints() != null ? in.endpoints() : d.endpoints();

        VelocityConfig.Endpoints.WebSocket wsIn = endpointsIn.webSocket() != null
                ? endpointsIn.webSocket()
                : d.endpoints().webSocket();

        String bindHost = wsIn.bindHost();
        if (bindHost == null || bindHost.isBlank()) {
            Log.error("'endpoints.websocket.bind-host' must not be empty");
            bindHost = d.endpoints().webSocket().bindHost();
            ok = false;
        } else {
            bindHost = bindHost.trim();
            if (bindHost.startsWith("ws://") || bindHost.startsWith("wss://")) {
                if (websocketMode) {
                    Log.warn("'endpoints.websocket.bind-host' must NOT include ws:// or wss://");
                }
                bindHost = bindHost.replaceFirst("^wss?://", "");
            }
        }

        int bindPort = wsIn.bindPort();
        if (bindPort <= 0 || bindPort > 65535) {
            Log.error("'endpoints.websocket.bind-port' must be between 1 and 65535");
            bindPort = d.endpoints().webSocket().bindPort();
            ok = false;
        }

        VelocityConfig.Endpoints.Redis redisIn = endpointsIn.redis() != null
                ? endpointsIn.redis()
                : d.endpoints().redis();

        String redisHost = redisIn.host();
        if (redisHost == null || redisHost.isBlank()) {
            Log.error("'endpoints.redis.host' must not be empty");
            redisHost = d.endpoints().redis().host();
            ok = false;
        } else {
            redisHost = redisHost.trim();
            if (redisHost.startsWith("redis://") || redisHost.startsWith("rediss://")) {
                if (redisMode) {
                    Log.warn("'endpoints.redis.host' must NOT include redis:// or rediss://");
                }
                redisHost = redisHost.replaceFirst("^rediss?://", "");
            }
        }

        int redisPort = redisIn.port();
        if (redisPort <= 0 || redisPort > 65535) {
            Log.error("'endpoints.redis.port' must be between 1 and 65535");
            redisPort = d.endpoints().redis().port();
            ok = false;
        }

        String redisUsername = redisIn.username() == null ? "" : redisIn.username().trim();
        String redisPassword = redisIn.password() == null ? "" : redisIn.password();

        VelocityConfig.Endpoints endpointsOut = new VelocityConfig.Endpoints(
                new VelocityConfig.Endpoints.WebSocket(bindHost, bindPort),
                new VelocityConfig.Endpoints.Redis(redisHost, redisPort, redisUsername, redisPassword));

        VelocityConfig.Heartbeat hbIn = in.heartbeat() != null ? in.heartbeat() : d.heartbeat();
        int appPing = hbIn.appPingSeconds();
        int stale = hbIn.staleAfterSeconds();
        if (appPing <= 0) {
            Log.error("'heartbeat.app-ping-seconds' must be > 0");
            appPing = d.heartbeat().appPingSeconds();
            ok = false;
        }
        if (stale < appPing) {
            Log.error("'heartbeat.stale-after-seconds' must be >= 'heartbeat.app-ping-seconds'");
            stale = Math.max(appPing, d.heartbeat().staleAfterSeconds());
            ok = false;
        }
        VelocityConfig.Heartbeat hbOut = new VelocityConfig.Heartbeat(appPing, stale);

        VelocityConfig.Timeouts toIn = in.timeouts() != null ? in.timeouts() : d.timeouts();
        int registerTimeout = toIn.registerTimeout();
        if (registerTimeout <= 0) {
            Log.error("'timeouts.register-timeout' must be > 0");
            registerTimeout = d.timeouts().registerTimeout();
            ok = false;
        }
        int pingTimeout = toIn.pingTimeout();
        if (pingTimeout <= 0) {
            Log.error("'timeouts.ping-timeout' must be > 0");
            pingTimeout = d.timeouts().pingTimeout();
            ok = false;
        }
        VelocityConfig.Timeouts toOut = new VelocityConfig.Timeouts(registerTimeout, pingTimeout);

        VelocityConfig.Limits limitsIn = in.limits() != null ? in.limits() : d.limits();
        int maxCon = limitsIn.maxConnections();
        int maxMsg = limitsIn.maxMessageSizeBytes();
        int inboundPerSec = limitsIn.inboundMessagesSec();
        if (maxCon <= 0) {
            Log.error("'limits.max-connections' must be positive");
            maxCon = d.limits().maxConnections();
            ok = false;
        }
        if (maxMsg < 1024) {
            Log.error("'limits.max-message-size-bytes' too small");
            maxMsg = Math.max(1024, d.limits().maxMessageSizeBytes());
            ok = false;
        }
        if (inboundPerSec <= 0) {
            Log.error("'limits.inbound-messages-per-sec' must be positive");
            inboundPerSec = d.limits().inboundMessagesSec();
            ok = false;
        }
        VelocityConfig.Limits limitsOut = new VelocityConfig.Limits(inboundPerSec, maxCon, maxMsg);

        VelocityConfig.Security secIn = in.security() != null ? in.security() : d.security();
        TlsMode tlsMode = secIn.tlsMode() != null ? secIn.tlsMode() : d.security().tlsMode();
        if (secIn.tlsMode() == null) {
            Log.error("'security.tls-mode' must be set");
            ok = false;
        }

        boolean requireAuth = secIn.requireAuth();
        int authTimeout = secIn.authTimeoutSeconds();
        if (authTimeout <= 0) {
            Log.error("'security.auth-timeout-seconds' must be > 0");
            authTimeout = d.security().authTimeoutSeconds();
            ok = false;
        }

        String keystorePath = emptyToNull(secIn.keystorePath());
        String keystorePassword = secIn.keystorePassword();
        String keystoreType = (secIn.keystoreType() == null || secIn.keystoreType().isBlank())
                ? d.security().keystoreType()
                : secIn.keystoreType().trim();

        if (!requireAuth) {
            Log.warn("Authentication is disabled! This is insecure and should not be used");
        }

        if (websocketMode && tlsMode == TlsMode.PLAIN && requireAuth) {
            Log.warn("'tls-mode=PLAIN' with 'require-auth=true' is unusual. consider TLS");
        }

        if (endpointType == EndpointType.WEBSOCKET && tlsMode == TlsMode.STRICT) {
            if (keystorePath == null || keystorePath.isBlank()) {
                Log.error("'security.keystore-path' is required in STRICT mode");
                ok = false;
                keystorePath = d.security().keystorePath();
            }
            if (keystorePassword == null) {
                Log.error("'security.keystore-password' is required in STRICT mode");
                ok = false;
                keystorePassword = d.security().keystorePassword();
            }
            if (keystoreType == null || keystoreType.isBlank()) {
                Log.error("security.keystore-type is required in STRICT mode");
                ok = false;
                keystoreType = d.security().keystoreType();
            }
        }

        VelocityConfig.Security secOut = new VelocityConfig.Security(
                requireAuth, authTimeout, tlsMode, keystorePath, keystorePassword, keystoreType);

        VelocityConfig out = new VelocityConfig(
                in.actAsClient(),
                serverId,
                endpointType,
                endpointsOut,
                hbOut,
                secOut,
                toOut,
                limitsOut,
                in.debug());
        return new Result<>(out, ok);
    }

    private static String emptyToNull(String s) {
        return (s != null && s.isBlank()) ? null : s;
    }
}
