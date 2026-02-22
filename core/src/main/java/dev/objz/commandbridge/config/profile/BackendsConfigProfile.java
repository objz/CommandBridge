package dev.objz.commandbridge.config.profile;

import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.logging.Log;

public final class BackendsConfigProfile implements ConfigProfile<BackendsConfig> {
    @Override
    public BackendsConfig defaults() {
        return BackendsConfig.defaults();
    }

    @Override
    public Result<BackendsConfig> normalize(BackendsConfig in) {
        BackendsConfig d = defaults();
        boolean ok = true;

        String clientId = in.clientId();
        if (clientId == null || clientId.isBlank()) {
            Log.warn("'client-id' missing or blank");
            clientId = d.clientId();
            ok = false;
        } else {
            clientId = clientId.trim();
        }

        EndpointType endpointType = in.endpointType();
        if (endpointType == null) {
            Log.error("'endpoint-type' must be set");
            endpointType = d.endpointType();
            ok = false;
        }

        BackendsConfig.Endpoints endpointsIn = in.endpoints() != null ? in.endpoints() : d.endpoints();

        BackendsConfig.Endpoints.WebSocket wsIn = endpointsIn.websocket() != null
                ? endpointsIn.websocket()
                : d.endpoints().websocket();

        String wsHost = wsIn.host();
        if (wsHost == null || wsHost.isBlank()) {
            Log.error("'endpoints.websocket.host' must not be empty");
            wsHost = d.endpoints().websocket().host();
            ok = false;
        } else {
            wsHost = wsHost.trim();
            if (wsHost.startsWith("ws://") || wsHost.startsWith("wss://")) {
                Log.warn("'endpoints.websocket.host' must NOT include ws:// or wss://");
                wsHost = wsHost.replaceFirst("^wss?://", "");
            }
        }

        int wsPort = wsIn.port();
        if (wsPort <= 0 || wsPort > 65535) {
            Log.error("'endpoints.websocket.port' must be between 1 and 65535");
            wsPort = d.endpoints().websocket().port();
            ok = false;
        }

        BackendsConfig.Endpoints.Redis redisIn = endpointsIn.redis() != null
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
                Log.warn("'endpoints.redis.host' must NOT include redis:// or rediss://");
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

        BackendsConfig.Endpoints endpointsOut = new BackendsConfig.Endpoints(
                new BackendsConfig.Endpoints.WebSocket(wsHost, wsPort),
                new BackendsConfig.Endpoints.Redis(redisHost, redisPort, redisUsername, redisPassword));

        BackendsConfig.Limits limitsIn = in.limits() != null ? in.limits() : d.limits();
        int inboundMessagesSec = limitsIn.inboundMessagesSec();
        if (inboundMessagesSec <= 0) {
            Log.error("'limits.inbound-messages-per-sec' must be positive");
            inboundMessagesSec = d.limits().inboundMessagesSec();
            ok = false;
        }
        BackendsConfig.Limits limitsOut = new BackendsConfig.Limits(inboundMessagesSec);

        BackendsConfig.Timeouts timeoutsIn = in.timeouts() != null ? in.timeouts() : d.timeouts();
        int authTimeout = timeoutsIn.authTimeout();
        int reconnectTimeout = timeoutsIn.reconnectTimeout();
        int reconnectInterval = timeoutsIn.reconnectInterval();
        if (authTimeout <= 0) {
            Log.error("'timeouts.auth-timeout' must be > 0");
            authTimeout = d.timeouts().authTimeout();
            ok = false;
        }
        if (reconnectTimeout <= 0) {
            Log.error("'timeouts.reconnect-timeout' must be > 0");
            reconnectTimeout = d.timeouts().reconnectTimeout();
            ok = false;
        }
        if (reconnectInterval <= 0) {
            Log.error("'timeouts.reconnect-interval' must be > 0");
            reconnectInterval = d.timeouts().reconnectInterval();
            ok = false;
        }
        BackendsConfig.Timeouts timeoutsOut = new BackendsConfig.Timeouts(authTimeout, reconnectTimeout,
                reconnectInterval);

        BackendsConfig.Security secIn = in.security() != null ? in.security() : d.security();
        TlsMode tlsMode = secIn.tlsMode() != null ? secIn.tlsMode() : d.security().tlsMode();
        if (secIn.tlsMode() == null) {
            Log.error("'security.tls-mode' must be set");
            ok = false;
        }

        String tlsPin = (secIn.tlsPin() != null && !secIn.tlsPin().isBlank())
                ? secIn.tlsPin().trim()
                : d.security().tlsPin();

        String secret = secIn.secret();
        if (secret == null || secret.isBlank()) {
            Log.error("'security.secret' must not be empty");
            secret = d.security().secret();
            ok = false;
        } else if (secret.toLowerCase().contains("change-me")) {
            Log.warn("'security.secret' contains 'change-me'. replace with real key");
        }

        Boolean requireAuth = secIn.requireAuth();
        if (requireAuth == null) {
            Log.warn("'security.require-auth' must be set");
            requireAuth = d.security().requireAuth();
        } else if (Boolean.FALSE.equals(requireAuth)) {
            Log.warn("Authentication is disabled! This is insecure and should not be used");
        }

        if (endpointType == EndpointType.WEBSOCKET && tlsMode == TlsMode.PLAIN && Boolean.TRUE.equals(requireAuth)) {
            Log.warn("'tls-mode=PLAIN' with 'require-auth=true' is unusual. consider TLS");
        }

        BackendsConfig.Security secOut = new BackendsConfig.Security(tlsMode, tlsPin, secret, requireAuth);

        BackendsConfig out = new BackendsConfig(
                clientId,
                endpointType,
                endpointsOut,
                secOut,
                timeoutsOut,
                limitsOut,
                in.debug());

        return new Result<>(out, ok);
    }
}
