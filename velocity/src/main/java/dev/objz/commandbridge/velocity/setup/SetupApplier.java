package dev.objz.commandbridge.velocity.setup;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.VelocityConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Turns a {@link SetupPlan} into the proxy's config, each server's config, and the files written on confirm. */
/** Expects a plan already passed through {@link SetupPlan#withCurrentPasswords}. */
public final class SetupApplier {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path dataDir;
    private final Path velocityToml;

    public SetupApplier(Path dataDir, Path velocityToml) {
        this.dataDir = dataDir;
        this.velocityToml = velocityToml;
    }

    public VelocityConfig proxyConfig(VelocityConfig current, SetupPlan plan) {
        var ws = current.endpoints().webSocket();
        var redis = current.endpoints().redis();
        if (plan.endpointType() == EndpointType.WEBSOCKET)
            ws = new VelocityConfig.Endpoints.WebSocket(ws.bindHost(), plan.port());
        if (plan.redis() != null)
            redis = new VelocityConfig.Endpoints.Redis(plan.redis().host(), plan.redis().port(),
                    plan.redis().username(), plan.redis().password());
        var sec = current.security();
        if (plan.keystore() != null)
            sec = new VelocityConfig.Security(plan.tlsMode(), plan.keystore().path(), plan.keystore().password(),
                    plan.keystore().type());
        else
            sec = new VelocityConfig.Security(plan.tlsMode(), sec.keystorePath(), sec.keystorePassword(), sec.keystoreType());
        return new VelocityConfig(current.actAsClient(), plan.serverId(), plan.endpointType(),
                new VelocityConfig.Endpoints(ws, redis), sec, current.timeouts(), current.tasks(), current.debug());
    }

    public BackendsConfig clientConfig(SetupPlan plan, SetupPlan.Server server, String secret, String tlsPin) {
        BackendsConfig d = BackendsConfig.defaults();
        var ws = plan.endpointType() == EndpointType.WEBSOCKET
                ? new BackendsConfig.Endpoints.WebSocket(plan.address().trim(), plan.port())
                : d.endpoints().websocket();
        var redis = plan.redis() != null
                ? new BackendsConfig.Endpoints.Redis(plan.redis().host(), plan.redis().port(),
                        plan.redis().username(), Objects.requireNonNullElse(plan.redis().password(), ""))
                : d.endpoints().redis();
        return new BackendsConfig(server.id(), plan.endpointType(), new BackendsConfig.Endpoints(ws, redis),
                new BackendsConfig.Security(plan.tlsMode(), tlsPin == null ? "" : tlsPin, secret),
                d.timeouts(), false);
    }

    public boolean needsRestart(VelocityConfig current, SetupPlan plan) {
        VelocityConfig next = proxyConfig(current, plan);
        return !Objects.equals(current.serverId(), next.serverId())
                || current.endpointType() != next.endpointType()
                || !Objects.equals(current.endpoints(), next.endpoints())
                || !Objects.equals(current.security(), next.security());
    }

    /** Writes config.yml, the new servers in velocity.toml and data/setup.json. Returns what went wrong. */
    public List<String> apply(VelocityConfig current, SetupPlan plan) {
        List<String> errors = new ArrayList<>();
        if (!new ConfigManager<>(dataDir, VelocityConfig.class).save(proxyConfig(current, plan)))
            errors.add("Could not write config.yml");

        Map<String, String> added = new LinkedHashMap<>();
        plan.servers().stream().filter(SetupPlan.Server::addToVelocity).forEach(s -> added.put(s.id(), s.address()));
        try {
            if (!added.isEmpty())
                VelocityToml.addServers(velocityToml, added);
        } catch (Exception e) {
            errors.add("Could not add servers to velocity.toml: " + e.getMessage());
        }

        try {
            Path file = dataDir.resolve("data").resolve("setup.json");
            Files.createDirectories(file.getParent());
            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("address", plan.address());
            saved.put("servers", plan.servers().stream()
                    .map(s -> Map.of("id", s.id(), "address", s.address())).toList());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), saved);
        } catch (Exception e) {
            errors.add("Could not write data/setup.json: " + e.getMessage());
        }
        return errors;
    }
}
