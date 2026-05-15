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
        if (isBlank(serverId)) {
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

        VelocityConfig.Endpoints endpointsDefaults = d.endpoints();
        VelocityConfig.Endpoints endpointsIn = in.endpoints() != null ? in.endpoints() : endpointsDefaults;

        VelocityConfig.Endpoints.WebSocket wsIn = endpointsIn.webSocket() != null
                ? endpointsIn.webSocket()
                : endpointsDefaults.webSocket();

        String bindHost = trimToNull(wsIn.bindHost());
        if (bindHost == null) {
            Log.error("'endpoints.websocket.bind-host' must not be empty");
            bindHost = endpointsDefaults.webSocket().bindHost();
            ok = false;
        } else {
            if (bindHost.startsWith("ws://") || bindHost.startsWith("wss://")) {
                Log.warn("'endpoints.websocket.bind-host' must NOT include ws:// or wss://");
                bindHost = bindHost.replaceFirst("^wss?://", "");
            }
        }

        int bindPort = wsIn.bindPort();
        if (bindPort <= 0 || bindPort > 65535) {
            Log.error("'endpoints.websocket.bind-port' must be between 1 and 65535");
            bindPort = endpointsDefaults.webSocket().bindPort();
            ok = false;
        }

        VelocityConfig.Endpoints.Redis redisIn = endpointsIn.redis() != null
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

        VelocityConfig.Endpoints endpointsOut = new VelocityConfig.Endpoints(
                new VelocityConfig.Endpoints.WebSocket(bindHost, bindPort),
                new VelocityConfig.Endpoints.Redis(redisHost, redisPort, redisUsername, redisPassword));

        VelocityConfig.Timeouts timeoutsDefaults = d.timeouts();
        VelocityConfig.Timeouts toIn = in.timeouts() != null ? in.timeouts() : timeoutsDefaults;

        int registerTimeout = toIn.registerTimeout();
        if (registerTimeout <= 0) {
            Log.error("'timeouts.register-timeout' must be > 0");
            registerTimeout = timeoutsDefaults.registerTimeout();
            ok = false;
        }

        int pingTimeout = toIn.pingTimeout();
        if (pingTimeout <= 0) {
            Log.error("'timeouts.ping-timeout' must be > 0");
            pingTimeout = timeoutsDefaults.pingTimeout();
            ok = false;
        }

        VelocityConfig.Timeouts toOut = new VelocityConfig.Timeouts(registerTimeout, pingTimeout);

        VelocityConfig.Tasks tasksDefaults = d.tasks();
        VelocityConfig.Tasks tasksIn = in.tasks() != null ? in.tasks() : tasksDefaults;

        int expireAfter = tasksIn.expireAfter();
        if (expireAfter < 0) {
            Log.error("'tasks.expire-after' must be >= 0 (0 disables expiry)");
            expireAfter = tasksDefaults.expireAfter();
            ok = false;
        }

        VelocityConfig.Tasks tasksOut = new VelocityConfig.Tasks(expireAfter);

        VelocityConfig.Security securityDefaults = d.security();
        VelocityConfig.Security secIn = in.security() != null ? in.security() : securityDefaults;

        TlsMode tlsMode = secIn.tlsMode() != null ? secIn.tlsMode() : securityDefaults.tlsMode();
        if (secIn.tlsMode() == null) {
            Log.error("'security.tls-mode' must be set");
            ok = false;
        }

        String keystorePath = trimToNull(secIn.keystorePath());
        String keystorePassword = secIn.keystorePassword();
        String keystoreType = isBlank(secIn.keystoreType())
                ? securityDefaults.keystoreType()
                : secIn.keystoreType().trim();

        if (websocketMode && tlsMode == TlsMode.PLAIN) {
            Log.warn("'tls-mode=PLAIN' disables transport encryption. use TOFU/STRICT in production");
        }

        if (websocketMode && tlsMode == TlsMode.STRICT) {
            if (keystorePath == null) {
                Log.error("'security.keystore-path' is required in STRICT mode");
                ok = false;
                keystorePath = securityDefaults.keystorePath();
            }

            if (isBlank(keystorePassword)) {
                Log.error("'security.keystore-password' is required in STRICT mode");
                ok = false;
                keystorePassword = securityDefaults.keystorePassword();
            }

            if (isBlank(keystoreType)) {
                Log.error("security.keystore-type is required in STRICT mode");
                ok = false;
                keystoreType = securityDefaults.keystoreType();
            }
        }

        VelocityConfig.Security secOut = new VelocityConfig.Security(tlsMode, keystorePath, keystorePassword, keystoreType);

        VelocityConfig out = new VelocityConfig(
                in.actAsClient(),
                serverId,
                endpointType,
                endpointsOut,
                secOut,
                toOut,
                tasksOut,
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
