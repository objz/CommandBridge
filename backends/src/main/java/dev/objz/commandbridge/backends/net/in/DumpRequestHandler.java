package dev.objz.commandbridge.backends.net.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class DumpRequestHandler extends InboundHandler {

    private final Supplier<BackendsConfig> configSupplier;
    private final Supplier<List<CommandStub>> commandSupplier;
    private final Supplier<Integer> playerCountSupplier;
    private final Supplier<String> clientIdSupplier;
    private final Location location;

    public DumpRequestHandler(
            Supplier<BackendsConfig> configSupplier,
            Supplier<List<CommandStub>> commandSupplier,
            Supplier<Integer> playerCountSupplier,
            Supplier<String> clientIdSupplier,
            Location location) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
        this.commandSupplier = Objects.requireNonNull(commandSupplier);
        this.playerCountSupplier = Objects.requireNonNull(playerCountSupplier);
        this.clientIdSupplier = Objects.requireNonNull(clientIdSupplier);
        this.location = Objects.requireNonNull(location);
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        try {
            ObjectNode payload = Envelope.MAPPER.createObjectNode();
            payload.put("schemaVersion", 1);
            payload.put("generatedAt", Instant.now().toString());
            payload.put("clientId", safe(clientIdSupplier.get()));
            payload.put("location", location.name());
            payload.put("platform", location == Location.VELOCITY ? "VELOCITY_BACKEND" : "BACKEND");

            ObjectNode runtime = payload.putObject("runtime");
            runtime.put("os", System.getProperty("os.name"));
            runtime.put("osArch", System.getProperty("os.arch"));
            runtime.put("javaVersion", System.getProperty("java.version"));
            Integer players = playerCountSupplier.get();
            runtime.put("playerCount", players != null ? Math.max(0, players) : 0);

            BackendsConfig cfg = configSupplier.get();
            JsonNode cfgNode = cfg == null
                    ? JsonNodeFactory.instance.objectNode()
                    : Envelope.MAPPER.valueToTree(cfg);
            payload.set("config", sanitize(cfgNode));

            ArrayNode commands = payload.putArray("registeredCommands");
            for (CommandStub cmd : commandSupplier.get()) {
                if (cmd == null) {
                    continue;
                }
                ObjectNode command = commands.addObject();
                command.put("name", safe(cmd.name()));

                ArrayNode aliases = command.putArray("aliases");
                if (cmd.aliases() != null) {
                    cmd.aliases().stream().filter(a -> a != null && !a.isBlank()).forEach(aliases::add);
                }

                command.put("description", safe(cmd.description()));
                command.put("argumentCount", cmd.args() != null ? cmd.args().size() : 0);
            }

            reply(endpoint, env, MessageType.DUMP_RESPONSE, payload)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send DUMP_RESPONSE: {}", ex.toString());
                        return null;
                    });
        } catch (Exception ex) {
            Log.error(ex, "Failed to build DUMP_RESPONSE payload");
        }
    }

    private static JsonNode sanitize(JsonNode node) {
        if (node == null || node.isNull()) {
            return JsonNodeFactory.instance.nullNode();
        }

        if (node.isObject()) {
            ObjectNode out = JsonNodeFactory.instance.objectNode();
            for (var it = node.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = entry.getKey();
                if (isSensitive(key)) {
                    out.put(key, "<redacted>");
                } else {
                    out.set(key, sanitize(entry.getValue()));
                }
            }
            return out;
        }

        if (node.isArray()) {
            ArrayNode out = JsonNodeFactory.instance.arrayNode();
            for (JsonNode child : node) {
                out.add(sanitize(child));
            }
            return out;
        }

        return node.deepCopy();
    }

    private static boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }

        String lower = key.toLowerCase(Locale.ROOT);
        return lower.equals("secret")
                || lower.contains("password")
                || lower.contains("token")
                || lower.contains("apikey")
                || lower.contains("api-key")
                || lower.contains("tls-pin");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
