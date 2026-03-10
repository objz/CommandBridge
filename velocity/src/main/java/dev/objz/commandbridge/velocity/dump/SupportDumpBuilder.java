package dev.objz.commandbridge.velocity.dump;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.util.BuildMeta;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class SupportDumpBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final RegistrationManager registrations;
    private final SessionHub sessions;
    private final ScriptManager scripts;
    private final VelocityConfig config;
    private final List<JsonNode> remoteSnapshots;

    public SupportDumpBuilder(
            RegistrationManager registrations,
            SessionHub sessions,
            ScriptManager scripts,
            VelocityConfig config) {
        this(registrations, sessions, scripts, config, List.of());
    }

    public SupportDumpBuilder(
            RegistrationManager registrations,
            SessionHub sessions,
            ScriptManager scripts,
            VelocityConfig config,
            List<JsonNode> remoteSnapshots) {
        this.registrations = registrations;
        this.sessions = sessions;
        this.scripts = scripts;
        this.config = config;
        this.remoteSnapshots = remoteSnapshots == null ? List.of() : List.copyOf(remoteSnapshots);
    }

    public String buildJson() throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", 1);
        root.put("generatedAt", Instant.now().toString());
        root.put("generatedBy", "velocity:/cb dump");

        ObjectNode plugin = root.putObject("plugin");
        plugin.put("name", "CommandBridge");
        plugin.put("platform", "VELOCITY");
        plugin.put("version", BuildMeta.VERSION);

        root.set("runtime", runtimeNode());
        root.set("config", configNode());
        root.set("scripts", scriptsNode());
        root.set("network", networkNode());
        root.set("registrations", registrationNode());
        root.set("topology", topologyNode());
        root.set("remoteClients", remoteClientsNode());

        ArrayNode exclusions = root.putArray("privacy");
        exclusions.add("player usernames and UUID lists are not exported");
        exclusions.add("security secrets, passwords, pins and tokens are redacted");

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private ObjectNode runtimeNode() {
        ObjectNode runtime = MAPPER.createObjectNode();
        runtime.put("os", System.getProperty("os.name"));
        runtime.put("osArch", System.getProperty("os.arch"));
        runtime.put("javaVersion", System.getProperty("java.version"));
        runtime.put("javaVendor", System.getProperty("java.vendor"));
        runtime.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        runtime.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();

        ObjectNode memory = runtime.putObject("memory");
        memory.put("totalBytes", totalMemory);
        memory.put("freeBytes", freeMemory);
        memory.put("usedBytes", totalMemory - freeMemory);
        memory.put("maxBytes", maxMemory);

        return runtime;
    }

    private JsonNode configNode() {
        JsonNode raw = MAPPER.valueToTree(config);
        return DumpSanitizer.sanitize(raw);
    }

    private ObjectNode scriptsNode() {
        ObjectNode scriptsNode = MAPPER.createObjectNode();
        scriptsNode.put("directory", scripts.scriptsDir().toString());
        scriptsNode.put("loaded", scripts.loaded().size());
        scriptsNode.put("enabled", scripts.enabled().size());
        scriptsNode.put("disabled", scripts.disabled().size());
        scriptsNode.put("validationErrors", scripts.errors());

        ArrayNode files = scriptsNode.putArray("files");
        for (ScriptFileMeta file : listScriptFiles(scripts.scriptsDir())) {
            ObjectNode fileNode = files.addObject();
            fileNode.put("name", file.name());
            fileNode.put("sizeBytes", file.sizeBytes());
            fileNode.put("modifiedAt", file.modifiedAt());
        }

        ArrayNode entries = scriptsNode.putArray("entries");
        int nullEntries = 0;
        for (Script script : scripts.loaded()) {
            if (script == null) {
                nullEntries++;
                continue;
            }
            JsonNode raw = MAPPER.valueToTree(script);
            entries.add(DumpSanitizer.sanitize(raw));
        }
        scriptsNode.put("nullEntries", nullEntries);

        return scriptsNode;
    }

    private ObjectNode networkNode() {
        ObjectNode network = MAPPER.createObjectNode();
        ArrayNode clients = network.putArray("clients");

        int total = 0;
        int authenticated = 0;
        int connected = 0;

        for (ClientSession session : sessions) {
            total++;
            if (session.status() == AuthStatus.AUTH_OK) {
                authenticated++;
            }

            boolean isConnected = session.endpoint() != null && session.endpoint().isOpen();
            if (isConnected) {
                connected++;
            }

            ObjectNode client = clients.addObject();
            client.put("id", safe(session.id()));
            client.put("location", session.location() != null ? session.location().name() : "UNKNOWN");
            client.put("status", session.status() != null ? session.status().name() : "UNKNOWN");
            client.put("connected", isConnected);
            client.put("endpoint", session.endpoint() != null ? safe(session.endpoint().describe()) : "unknown");

            Set<Script> targeted = registrations.getScriptsForSession(session);
            client.put("targetedScriptCount", targeted.size());
            ArrayNode targetedNames = client.putArray("targetedScripts");
            targeted.stream()
                    .map(Script::name)
                    .filter(name -> name != null && !name.isBlank())
                    .sorted()
                    .forEach(targetedNames::add);
        }

        ObjectNode summary = network.putObject("summary");
        summary.put("totalClients", total);
        summary.put("authenticatedClients", authenticated);
        summary.put("connectedClients", connected);

        return network;
    }

    private ObjectNode registrationNode() {
        ObjectNode node = MAPPER.createObjectNode();

        int loadedScriptCount = 0;
        int enabledScriptCount = 0;
        int localLoadedScriptCount = 0;
        int localEnabledScriptCount = 0;
        int totalTargetMappings = 0;

        Set<String> velocityTargets = new HashSet<>();
        Set<String> backendTargets = new HashSet<>();

        Set<String> connectedTargetKeys = connectedTargetKeys();

        ArrayNode scriptsArray = node.putArray("scripts");

        for (Script script : scripts.loaded()) {
            if (script == null) {
                continue;
            }

            loadedScriptCount++;
            if (script.enabled()) {
                enabledScriptCount++;
            }

            List<IdMapping> targets = script.register() == null ? List.of() : script.register();
            boolean localTargeted = false;

            ObjectNode scriptNode = scriptsArray.addObject();
            scriptNode.put("name", safe(script.name()));
            scriptNode.put("enabled", script.enabled());
            scriptNode.put("version", script.version());
            scriptNode.put("description", safe(script.description()));
            scriptNode.put("commandCount", script.commands() != null ? script.commands().size() : 0);
            scriptNode.put("argCount", script.args() != null ? script.args().size() : 0);
            scriptNode.put("targetCount", targets.size());

            ArrayNode aliasesArray = scriptNode.putArray("aliases");
            if (script.aliases() != null) {
                script.aliases().stream().filter(a -> a != null && !a.isBlank()).forEach(aliasesArray::add);
            }

            ArrayNode commandTemplates = scriptNode.putArray("commandTemplates");
            if (script.commands() != null) {
                script.commands().stream()
                        .filter(cmd -> cmd != null)
                        .map(CmdMapping::command)
                        .filter(cmd -> cmd != null && !cmd.isBlank())
                        .forEach(commandTemplates::add);
            }

            ArrayNode targetArray = scriptNode.putArray("targets");
            for (IdMapping target : targets) {
                if (target == null || target.id() == null || target.location() == null) {
                    continue;
                }

                totalTargetMappings++;

                boolean isLocal = target.location() == Location.VELOCITY && target.id().equals(config.serverId());
                boolean currentlyConnected = connectedTargetKeys.contains(targetKey(target.id(), target.location()));

                if (isLocal) {
                    localTargeted = true;
                } else if (target.location() == Location.VELOCITY) {
                    velocityTargets.add(target.id());
                } else {
                    backendTargets.add(target.id());
                }

                ObjectNode targetNode = targetArray.addObject();
                targetNode.put("id", target.id());
                targetNode.put("location", target.location().name());
                targetNode.put("local", isLocal);
                targetNode.put("connected", currentlyConnected);
            }

            if (localTargeted) {
                localLoadedScriptCount++;
                if (script.enabled()) {
                    localEnabledScriptCount++;
                }
            }
        }

        node.put("localServerId", config.serverId());
        node.put("loadedScriptCount", loadedScriptCount);
        node.put("enabledScriptCount", enabledScriptCount);
        node.put("localScriptCount", localEnabledScriptCount);
        node.put("localLoadedScriptCount", localLoadedScriptCount);
        node.put("totalTargetMappings", totalTargetMappings);
        node.put("remoteVelocityTargets", velocityTargets.size());
        node.put("remoteBackendTargets", backendTargets.size());

        ArrayNode velocityIds = node.putArray("velocityTargetIds");
        velocityTargets.stream().sorted().forEach(velocityIds::add);

        ArrayNode backendIds = node.putArray("backendTargetIds");
        backendTargets.stream().sorted().forEach(backendIds::add);

        return node;
    }

    private ObjectNode topologyNode() {
        ObjectNode topology = MAPPER.createObjectNode();
        ArrayNode nodes = topology.putArray("nodes");
        ArrayNode edges = topology.putArray("edges");

        Set<String> seenNodes = new HashSet<>();
        Set<String> seenEdges = new HashSet<>();
        Map<String, String> sessionNodeByTarget = new HashMap<>();

        String localNodeId = nodeId("velocity", Location.VELOCITY, config.serverId());
        addNode(nodes, seenNodes, localNodeId, "VELOCITY_LOCAL", config.serverId(), Location.VELOCITY, true, "AUTH_OK");

        for (ClientSession session : sessions) {
            String sessionId = safe(session.id());
            Location location = session.location();
            if (location == null || sessionId.isBlank()) {
                continue;
            }

            boolean connected = isConnected(session);
            String status = session.status() != null ? session.status().name() : "UNKNOWN";

            String sessionNodeId = nodeId("session", location, sessionId);
            addNode(nodes, seenNodes, sessionNodeId, "SESSION", sessionId, location, connected, status);
            addEdge(edges, seenEdges, localNodeId, sessionNodeId, "SESSION_LINK");
            sessionNodeByTarget.put(targetKey(sessionId, location), sessionNodeId);
        }

        for (Script script : scripts.loaded()) {
            if (script == null || script.name() == null || script.name().isBlank()) {
                continue;
            }

            String scriptNodeId = "script:" + script.name();
            addNode(nodes, seenNodes, scriptNodeId, "SCRIPT", script.name(), null, script.enabled(), null);
            addEdge(edges, seenEdges, localNodeId, scriptNodeId, "SCRIPT_OWNER");

            List<IdMapping> targets = script.register() == null ? List.of() : script.register();
            for (IdMapping target : targets) {
                if (target == null || target.id() == null || target.location() == null) {
                    continue;
                }

                boolean localTarget = target.location() == Location.VELOCITY && target.id().equals(config.serverId());
                String targetNodeId;

                if (localTarget) {
                    targetNodeId = localNodeId;
                } else {
                    targetNodeId = nodeId("target", target.location(), target.id());
                    boolean connected = sessionNodeByTarget.containsKey(targetKey(target.id(), target.location()));
                    addNode(nodes, seenNodes, targetNodeId, "TARGET", target.id(), target.location(), connected,
                            connected ? "ONLINE" : "OFFLINE");
                }

                addEdge(edges, seenEdges, scriptNodeId, targetNodeId, "SCRIPT_TARGET");

                String sessionNodeId = sessionNodeByTarget.get(targetKey(target.id(), target.location()));
                if (!localTarget && sessionNodeId != null) {
                    addEdge(edges, seenEdges, targetNodeId, sessionNodeId, "TARGET_SESSION");
                }
            }
        }

        ObjectNode stats = topology.putObject("stats");
        stats.put("nodeCount", nodes.size());
        stats.put("edgeCount", edges.size());
        stats.put("sessionCount", sessionNodeByTarget.size());

        return topology;
    }

    private ObjectNode remoteClientsNode() {
        ObjectNode remote = MAPPER.createObjectNode();
        ArrayNode entries = remote.putArray("entries");

        int collected = 0;
        int failed = 0;

        for (JsonNode snapshot : remoteSnapshots) {
            JsonNode sanitized = DumpSanitizer.sanitize(snapshot);
            entries.add(sanitized);
            if (sanitized.path("collected").asBoolean(false)) {
                collected++;
            } else {
                failed++;
            }
        }

        remote.put("requested", remoteSnapshots.size());
        remote.put("collected", collected);
        remote.put("failed", failed);
        return remote;
    }

    private Set<String> connectedTargetKeys() {
        Set<String> connectedTargets = new HashSet<>();
        connectedTargets.add(targetKey(config.serverId(), Location.VELOCITY));

        for (ClientSession session : sessions) {
            if (!isConnected(session)) {
                continue;
            }
            if (session.id() == null || session.location() == null) {
                continue;
            }
            connectedTargets.add(targetKey(session.id(), session.location()));
        }

        return connectedTargets;
    }

    private static void addNode(
            ArrayNode nodes,
            Set<String> seen,
            String id,
            String kind,
            String label,
            Location location,
            boolean connected,
            String status) {
        if (!seen.add(id)) {
            return;
        }

        ObjectNode node = nodes.addObject();
        node.put("id", id);
        node.put("kind", kind);
        node.put("label", safe(label));
        node.put("location", location != null ? location.name() : "UNKNOWN");
        node.put("connected", connected);
        node.put("status", status != null ? status : "UNKNOWN");
    }

    private static void addEdge(ArrayNode edges, Set<String> seen, String from, String to, String type) {
        String key = from + "->" + to + "#" + type;
        if (!seen.add(key)) {
            return;
        }

        ObjectNode edge = edges.addObject();
        edge.put("from", from);
        edge.put("to", to);
        edge.put("type", type);
    }

    private static String nodeId(String prefix, Location location, String id) {
        return prefix + ":" + (location != null ? location.name() : "UNKNOWN") + ":" + safe(id);
    }

    private static String targetKey(String id, Location location) {
        return (location != null ? location.name() : "UNKNOWN") + ":" + safe(id);
    }

    private static boolean isConnected(ClientSession session) {
        return session != null && session.endpoint() != null && session.endpoint().isOpen();
    }

    private static List<ScriptFileMeta> listScriptFiles(Path scriptsDir) {
        if (scriptsDir == null || !Files.isDirectory(scriptsDir)) {
            return List.of();
        }

        List<ScriptFileMeta> list = new ArrayList<>();
        try (Stream<Path> stream = Files.list(scriptsDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(SupportDumpBuilder::isYaml)
                    .forEach(path -> {
                        try {
                            list.add(new ScriptFileMeta(
                                    path.getFileName().toString(),
                                    Files.size(path),
                                    Files.getLastModifiedTime(path).toInstant().toString()));
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
            return List.of();
        }

        list.sort(Comparator.comparing(ScriptFileMeta::name));
        return list;
    }

    private static boolean isYaml(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ScriptFileMeta(String name, long sizeBytes, String modifiedAt) {
    }
}
