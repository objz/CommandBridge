package dev.objz.commandbridge.velocity.setup;

import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** What the setup page answered. {@code redis} and {@code keystore} are null when they don't apply. */
public record SetupPlan(String serverId, EndpointType endpointType, int port, String address, Redis redis,
        TlsMode tlsMode, Keystore keystore, List<Server> servers) {

    /** A null password means "keep the proxy's", see {@link #withCurrentPasswords}. */
    public record Redis(String host, int port, String username, String password) {
    }

    /** A null password means "keep the proxy's", see {@link #withCurrentPasswords}. */
    public record Keystore(String path, String type, String password) {
    }

    /** {@code address} is {@code host:port}; {@code addToVelocity} marks servers that aren't in velocity.toml yet. */
    public record Server(String id, String address, boolean addToVelocity) {
    }

    public static SetupPlan fromJson(JsonNode n) {
        JsonNode r = n.path("redis");
        JsonNode k = n.path("keystore");
        List<Server> servers = new ArrayList<>();
        n.path("servers").forEach(s -> servers.add(new Server(s.path("id").asText(""),
                s.path("address").asText(""), s.path("addToVelocity").asBoolean(false))));
        return new SetupPlan(
                n.path("serverId").asText(""),
                parse(EndpointType.class, n.path("endpointType").asText("")),
                n.path("port").asInt(0),
                n.path("address").asText(""),
                r.isObject() ? new Redis(r.path("host").asText(""), r.path("port").asInt(0),
                        r.path("username").asText(""), r.hasNonNull("password") ? r.path("password").asText() : null)
                        : null,
                parse(TlsMode.class, n.path("tlsMode").asText("")),
                k.isObject() ? new Keystore(k.path("path").asText(""), k.path("type").asText("PKCS12"),
                        k.hasNonNull("password") ? k.path("password").asText() : null) : null,
                servers);
    }

    /** The page never sees the proxy's passwords; a password it left out keeps the proxy's current one. */
    public SetupPlan withCurrentPasswords(VelocityConfig current) {
        Redis r = redis != null && redis.password() == null
                ? new Redis(redis.host(), redis.port(), redis.username(), current.endpoints().redis().password())
                : redis;
        Keystore k = keystore != null && keystore.password() == null
                ? new Keystore(keystore.path(), keystore.type(), current.security().keystorePassword())
                : keystore;
        return new SetupPlan(serverId, endpointType, port, address, r, tlsMode, k, servers);
    }

    public List<String> problems() {
        List<String> out = new ArrayList<>();
        if (serverId == null || !serverId.matches("^[a-z0-9-]{1,32}$"))
            out.add("The proxy name can use a to z, 0 to 9 and dashes, up to 32 characters.");
        if (endpointType == null)
            out.add("Pick how servers connect: WebSocket or Redis.");
        if (endpointType == EndpointType.WEBSOCKET) {
            if (port < 1 || port > 65535)
                out.add("The port must be between 1 and 65535.");
            if (address == null || address.isBlank())
                out.add("Enter the address servers use to reach the proxy.");
            else if (address.trim().equals("0.0.0.0"))
                out.add("Servers can't reach 0.0.0.0. Use the address they connect to.");
        }
        if (endpointType == EndpointType.REDIS
                && (redis == null || redis.host().isBlank() || redis.port() < 1 || redis.port() > 65535))
            out.add("Enter the Redis host and a port between 1 and 65535.");
        if (tlsMode == null)
            out.add("Pick an encryption mode.");
        if (tlsMode == TlsMode.STRICT && (keystore == null || keystore.path().isBlank()))
            out.add("Strict encryption needs a keystore path.");
        Set<String> seen = new HashSet<>();
        for (Server s : servers) {
            if (s.id().isBlank() || s.address().isBlank())
                out.add("Every server needs a name and an address.");
            else if (!seen.add(s.id()))
                out.add("Two servers are named " + s.id() + ".");
        }
        return out;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
