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
        if (isBlank(clientId)) {
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

        BackendsConfig.Endpoints endpointsDefaults = d.endpoints();
        BackendsConfig.Endpoints endpointsIn = in.endpoints() != null ? in.endpoints() : endpointsDefaults;

        BackendsConfig.Endpoints.WebSocket wsIn = endpointsIn.websocket() != null
                ? endpointsIn.websocket()
                : endpointsDefaults.websocket();

        String wsHost = trimToNull(wsIn.host());
        if (wsHost == null) {
            Log.error("'endpoints.websocket.host' must not be empty");
            wsHost = endpointsDefaults.websocket().host();
            ok = false;
        } else {
            if (wsHost.startsWith("ws://") || wsHost.startsWith("wss://")) {
                Log.warn("'endpoints.websocket.host' must NOT include ws:// or wss://");
                wsHost = wsHost.replaceFirst("^wss?://", "");
            }
        }

        int wsPort = wsIn.port();
        if (wsPort <= 0 || wsPort > 65535) {
            Log.error("'endpoints.websocket.port' must be between 1 and 65535");
            wsPort = endpointsDefaults.websocket().port();
            ok = false;
        }

        BackendsConfig.Endpoints.Redis redisIn = endpointsIn.redis() != null
                ? endpointsIn.redis()
                : endpointsDefaults.redis();

        String redisHost = trimToNull(redisIn.host());
        if (redisHost == null) {
            Log.error("'endpoints.redis.host' must not be empty");
            redisHost = endpointsDefaults.redis().host();
            ok = false;
        } else {
            if (redisHost.startsWith("redis://") || redisHost.startsWith("rediss://")) {
                Log.warn("'endpoints.redis.host' must NOT include redis:// or rediss://");
                redisHost = redisHost.replaceFirst("^rediss?://", "");
            }
        }

        int redisPort = redisIn.port();
        if (redisPort <= 0 || redisPort > 65535) {
            Log.error("'endpoints.redis.port' must be between 1 and 65535");
            redisPort = endpointsDefaults.redis().port();
            ok = false;
        }

        String redisUsername = redisIn.username() == null ? "" : redisIn.username().trim();
        String redisPassword = redisIn.password() == null ? "" : redisIn.password();

        BackendsConfig.Endpoints endpointsOut = new BackendsConfig.Endpoints(
                new BackendsConfig.Endpoints.WebSocket(wsHost, wsPort),
                new BackendsConfig.Endpoints.Redis(redisHost, redisPort, redisUsername, redisPassword));

        BackendsConfig.Timeouts timeoutsDefaults = d.timeouts();
        BackendsConfig.Timeouts timeoutsIn = in.timeouts() != null ? in.timeouts() : timeoutsDefaults;

        int authTimeout = timeoutsIn.authTimeout();
        int reconnectTimeout = timeoutsIn.reconnectTimeout();
        int reconnectInterval = timeoutsIn.reconnectInterval();

        if (authTimeout <= 0) {
            Log.error("'timeouts.auth-timeout' must be > 0");
            authTimeout = timeoutsDefaults.authTimeout();
            ok = false;
        }

        if (reconnectTimeout <= 0) {
            Log.error("'timeouts.reconnect-timeout' must be > 0");
            reconnectTimeout = timeoutsDefaults.reconnectTimeout();
            ok = false;
        }

        if (reconnectInterval <= 0) {
            Log.error("'timeouts.reconnect-interval' must be > 0");
            reconnectInterval = timeoutsDefaults.reconnectInterval();
            ok = false;
        }

        BackendsConfig.Timeouts timeoutsOut = new BackendsConfig.Timeouts(authTimeout, reconnectTimeout,
                reconnectInterval);

        BackendsConfig.Security securityDefaults = d.security();
        BackendsConfig.Security secIn = in.security() != null ? in.security() : securityDefaults;

        TlsMode tlsMode = secIn.tlsMode() != null ? secIn.tlsMode() : securityDefaults.tlsMode();
        if (secIn.tlsMode() == null) {
            Log.error("'security.tls-mode' must be set");
            ok = false;
        }

        String tlsPin = trimToNull(secIn.tlsPin());
        if (tlsPin == null) {
            tlsPin = securityDefaults.tlsPin();
        }

        String secret = secIn.secret();
        if (isBlank(secret)) {
            Log.error("'security.secret' must not be empty");
            secret = securityDefaults.secret();
            ok = false;
        } else if (secret.toLowerCase().contains("change-me")) {
            Log.warn("'security.secret' contains 'change-me'. replace with real key");
        }

        if (endpointType == EndpointType.WEBSOCKET && tlsMode == TlsMode.PLAIN) {
            Log.warn("'tls-mode=PLAIN' disables transport encryption. use TOFU/STRICT in production");
        }

        BackendsConfig.Security secOut = new BackendsConfig.Security(tlsMode, tlsPin, secret);

        BackendsConfig out = new BackendsConfig(
                clientId,
                endpointType,
                endpointsOut,
                secOut,
                timeoutsOut,
                in.debug());

        return new Result<>(out, ok);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
