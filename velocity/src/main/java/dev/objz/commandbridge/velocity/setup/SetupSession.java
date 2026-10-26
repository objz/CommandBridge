package dev.objz.commandbridge.velocity.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.PairCrypto;
import dev.objz.commandbridge.security.SecretLoader;
import dev.objz.commandbridge.security.TLS;
import dev.objz.commandbridge.velocity.editor.RelayChannel;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The setup page's session: it hands the page what the proxy knows, makes a pairing code per
 * server once the page sends a valid plan, and writes nothing until /cb setup confirm.
 */
public final class SetupSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";

    /** What /cb setup confirm reports back. */
    public record Result(boolean restartNeeded, List<String> errors) {
    }

    private final ProxyServer proxy;
    private final ConfigManager<VelocityConfig> configManager;
    private final SessionHub sessionHub;
    private final Path dataDir;
    private final SetupApplier applier;
    private final Map<String, PairOffer> offers = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> paired = new LinkedHashMap<>();
    private final Map<String, byte[]> clientConfigs = new HashMap<>();
    private volatile SetupPlan plan;
    private volatile JsonNode planJson;
    private volatile String staged;
    private volatile RelayChannel channel;

    private SetupSession(ProxyServer proxy, ConfigManager<VelocityConfig> configManager, SessionHub sessionHub,
            Path dataDir, Path velocityToml) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.sessionHub = sessionHub;
        this.dataDir = dataDir;
        this.applier = new SetupApplier(dataDir, velocityToml);
    }

    public static CompletableFuture<SetupSession> open(ProxyServer proxy, ConfigManager<VelocityConfig> configManager,
            SessionHub sessionHub, Path dataDir, Path velocityToml, Runnable onClosed) {
        SetupSession session = new SetupSession(proxy, configManager, sessionHub, dataDir, velocityToml);
        return RelayChannel.open("Setup", "setup/", session::handle, onClosed)
                .thenApply(channel -> {
                    session.channel = channel;
                    return session;
                });
    }

    public String url() {
        return channel.url();
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    /** Closes the page's channel. Codes stay valid until they run out, so servers can pair later. */
    public void close() {
        channel.close();
    }

    /** Ends every pairing code this session made, for when a new setup replaces it. */
    public synchronized void closeOffers() {
        offers.values().forEach(PairOffer::close);
        offers.clear();
    }

    /** Writes the plan staged under {@code code}; null when no plan waits for that code. */
    public synchronized Result confirm(String code) throws Exception {
        String s = staged;
        SetupPlan p = plan;
        if (s == null || p == null || code == null || !MessageDigest.isEqual(s.getBytes(StandardCharsets.US_ASCII),
                code.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            return null;
        }
        staged = null;
        VelocityConfig current = configManager.current();
        boolean restart = applier.needsRestart(current, p);
        List<String> errors = new ArrayList<>(applier.apply(current, p));
        for (SetupPlan.Server server : p.servers()) {
            if (server.addToVelocity() && proxy.getServer(server.id()).isEmpty()) {
                try {
                    proxy.registerServer(new ServerInfo(server.id(), address(server.address())));
                } catch (Exception e) {
                    errors.add("Could not register " + server.id() + ": " + e.getMessage());
                }
            }
        }
        if (!restart)
            configManager.reload();

        Map<String, String> configs = new LinkedHashMap<>();
        for (SetupPlan.Server server : p.servers()) {
            if (!paired.containsKey(server.id()) && clientConfigs.containsKey(server.id())) {
                BackendsConfig c = MAPPER.readValue(clientConfigs.get(server.id()), BackendsConfig.class);
                configs.put(server.id(), ConfigManager.render(BackendsConfig.class, c));
            }
        }
        Map<String, Object> applied = new LinkedHashMap<>();
        applied.put("type", "applied");
        applied.put("restartNeeded", restart);
        applied.put("errors", errors);
        applied.put("configs", configs);
        channel.send(applied);
        return new Result(restart, errors);
    }

    private synchronized void handle(JsonNode msg) throws Exception {
        switch (msg.path("type").asText("")) {
            case "hello" -> sendState();
            case "plan" -> plan(msg.path("plan"));
            case "stage" -> stage();
            case "unstage" -> {
                staged = null;
                sendState();
            }
            default -> Log.warn("Setup session {} got an unknown message type", channel.id());
        }
    }

    private void plan(JsonNode json) throws Exception {
        VelocityConfig current = configManager.current();
        SetupPlan next = SetupPlan.fromJson(json).withCurrentPasswords(current);
        List<String> problems = new ArrayList<>(next.problems());
        String pin = "";
        if (problems.isEmpty()) {
            try {
                pin = pin(next, dataDir);
            } catch (IllegalStateException e) {
                problems.add(e.getMessage());
            }
        }
        if (!problems.isEmpty()) {
            channel.send(Map.of("type", "plan-error", "problems", problems));
            return;
        }

        SetupPlan before = plan;
        boolean sameConnection = before != null && sameConnection(before, next);
        plan = next;
        planJson = json;
        staged = null;
        String secret = new SecretLoader(dataDir).loadOrCreate();
        boolean restart = applier.needsRestart(current, next);
        Map<String, ClientSession> connected = new HashMap<>();
        sessionHub.authenticated().forEach(s -> connected.put(s.id(), s));

        List<String> wanted = next.servers().stream().map(SetupPlan.Server::id).toList();
        offers.keySet().removeIf(id -> {
            boolean drop = !wanted.contains(id) || !sameConnection;
            if (drop)
                offers.get(id).close();
            return drop;
        });
        if (!sameConnection)
            paired.clear();
        paired.keySet().retainAll(wanted);

        for (SetupPlan.Server server : next.servers()) {
            BackendsConfig config = applier.clientConfig(next, server, secret, pin);
            clientConfigs.put(server.id(), MAPPER.writeValueAsBytes(config));
            ClientSession live = connected.get(server.id());
            if (!restart && live != null) {
                paired.put(server.id(), platform(live.platform().name(), null));
                continue;
            }
            if (paired.containsKey(server.id()) || offers.containsKey(server.id()))
                continue;
            PairOffer offer = new PairOffer(server.id(), next.serverId(), PairCrypto.newCode(),
                    clientConfigs.get(server.id()), hello -> onPaired(server.id(), hello));
            offers.put(server.id(), offer);
            offer.start().exceptionally(e -> {
                Log.warn("Could not offer a pairing code for {}: {}", server.id(), e.getMessage());
                return null;
            });
        }
        sendState();
    }

    private synchronized void onPaired(String id, JsonNode hello) {
        offers.remove(id);
        String platform = hello.path("platform").asText("");
        String version = hello.path("version").asText("");
        paired.put(id, platform(platform, version));
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "paired");
            msg.put("id", id);
            msg.put("platform", platform);
            msg.put("version", version);
            channel.send(msg);
        } catch (Exception e) {
            Log.warn("Setup session could not tell the page {} paired: {}", id, e.getMessage());
        }
    }

    private void stage() throws Exception {
        if (plan == null) {
            channel.send(Map.of("type", "plan-error", "problems", List.of("Finish the questions first.")));
            return;
        }
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++)
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        staged = code.toString();
        Log.info("The setup is waiting: run /cb setup confirm {} to apply it", staged);
        channel.send(Map.of("type", "staged", "code", staged));
    }

    private void sendState() throws Exception {
        Map<String, ClientSession> connected = new HashMap<>();
        sessionHub.authenticated().forEach(s -> connected.put(s.id(), s));
        List<Map<String, Object>> servers = new ArrayList<>();
        for (RegisteredServer rs : proxy.getAllServers()) {
            ServerInfo info = rs.getServerInfo();
            ClientSession live = connected.get(info.getName());
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", info.getName());
            s.put("address", info.getAddress().getHostString() + ":" + info.getAddress().getPort());
            s.put("connected", live != null);
            s.put("platform", live != null ? live.platform().name() : null);
            s.put("version", null);
            servers.add(s);
        }

        Map<String, String> codes = new LinkedHashMap<>();
        offers.forEach((id, offer) -> {
            if (offer.isOpen())
                codes.put(id, offer.code());
        });

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("type", "state");
        state.put("current", current(configManager.current()));
        state.put("servers", servers);
        state.put("saved", saved());
        state.put("plan", planJson);
        state.put("codes", codes);
        state.put("paired", paired);
        state.put("staged", staged);
        channel.send(state);
    }

    /** The proxy's settings as the page shows them; passwords stay on the proxy. */
    static Map<String, Object> current(VelocityConfig c) {
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("serverId", c.serverId());
        current.put("endpointType", c.endpointType().name());
        current.put("port", c.endpoints().webSocket().bindPort());
        current.put("bindHost", c.endpoints().webSocket().bindHost());
        var r = c.endpoints().redis();
        current.put("redis", Map.of("host", r.host(), "port", r.port(), "username", r.username()));
        current.put("tlsMode", c.security().tlsMode().name());
        // normalize leaves an unset keystore path null
        current.put("keystore", Map.of("path", Objects.requireNonNullElse(c.security().keystorePath(), ""),
                "type", c.security().keystoreType()));
        return current;
    }

    private JsonNode saved() {
        Path file = dataDir.resolve("data").resolve("setup.json");
        try {
            return Files.exists(file) ? MAPPER.readTree(file.toFile()) : null;
        } catch (Exception e) {
            Log.warn("Could not read data/setup.json: {}", e.getMessage());
            return null;
        }
    }

    static String pin(SetupPlan p, Path dataDir) {
        if (p.tlsMode() == TlsMode.STRICT)
            return TLS.keystoreSpkiPin(p.keystore().path(), p.keystore().type(), p.keystore().password());
        if (p.tlsMode() == TlsMode.TOFU) {
            // the TOFU keystore lives in data/, see TlsResolver
            String pin = TLS.readServerSpkiPin(dataDir.resolve("data"));
            return pin == null ? "" : pin;
        }
        return "";
    }

    private static boolean sameConnection(SetupPlan a, SetupPlan b) {
        return a.endpointType() == b.endpointType() && a.port() == b.port() && a.address().equals(b.address())
                && Objects.equals(a.redis(), b.redis()) && a.tlsMode() == b.tlsMode()
                && Objects.equals(a.keystore(), b.keystore());
    }

    private static Map<String, String> platform(String platform, String version) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("platform", platform);
        m.put("version", version);
        return m;
    }

    private static InetSocketAddress address(String hostPort) {
        int colon = hostPort.lastIndexOf(':');
        if (colon < 0)
            return new InetSocketAddress(hostPort, 25565);
        return new InetSocketAddress(hostPort.substring(0, colon), Integer.parseInt(hostPort.substring(colon + 1)));
    }
}
