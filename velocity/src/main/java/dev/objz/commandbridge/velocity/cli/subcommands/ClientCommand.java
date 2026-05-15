package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.out.ctx.PingRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.ui.Report;
import dev.objz.commandbridge.velocity.ui.Report.Status;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import dev.objz.commandbridge.velocity.util.UserCache;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientCommand {

    private static final String SECTION = "Clients";

    private final SessionHub sessions;
    private final OutNode outNode;
    private final PlayerTracker playerTracker;
    private final UserCache userCache;
    private final Duration pingTimeout;

    public ClientCommand(SessionHub sessions, OutNode outNode, PlayerTracker playerTracker,
            UserCache userCache, VelocityConfig config) {
        this.sessions = Objects.requireNonNull(sessions);
        this.outNode = Objects.requireNonNull(outNode);
        this.playerTracker = Objects.requireNonNull(playerTracker);
        this.userCache = Objects.requireNonNull(userCache);
        this.pingTimeout = Duration.ofSeconds(config.timeouts().pingTimeout());
    }

    public void list(CommandSource sender) {
        List<ClientSession> authenticated = sessions.authenticated();
        Report report = Report.of(SECTION);

        if (authenticated.isEmpty()) {
            report.warn("No authenticated clients connected").send(sender);
            return;
        }

        report.section("Summary")
                .summary("Connected", authenticated.size(), Status.SUCCESS);

        report.section("Sections");
        for (ClientSession s : authenticated) {
            String id = s.id() != null ? s.id() : "unknown";
            String address = s.endpoint() != null ? s.endpoint().describe() : "unknown";
            String platform = s.location() != null ? s.location().name() : "unknown";
            int playerCount = playerTracker.playersOn(id).size();
            String subline = platform + " · " + address + " · " + playerCount
                    + " player" + (playerCount == 1 ? "" : "s");
            report.listItem(id, subline, Status.SUCCESS, null);
        }

        report.send(sender);
    }

    public void ping(CommandSource sender, String id) {
        if (id == null) {
            pingAll(sender);
        } else {
            pingOne(sender, id);
        }
    }

    public void players(CommandSource sender, String id) {
        ClientSession session = sessions.get(id).orElse(null);
        if (session == null) {
            Report.of("Players").error("Unknown client '" + id + "'").send(sender);
            return;
        }
        Set<UUID> online = playerTracker.playersOn(id);

        Report report = Report.of("Players")
                .title(id)
                .blank();

        if (online.isEmpty()) {
            report.muted("(no players online)");
        } else {
            for (UUID uuid : online) {
                report.bullet(userCache.displayName(uuid));
            }
        }

        report.summarySection()
                .summary("Total", online.size(), Status.ACCENT);

        report.send(sender);
    }

    private void pingAll(CommandSource sender) {
        List<ClientSession> active = sessions.authenticated();
        if (active.isEmpty()) {
            Report.of("Ping").warn("No authenticated clients to ping").send(sender);
            return;
        }

        AtomicInteger completed = new AtomicInteger(0);
        ConcurrentHashMap<String, PingResult> results = new ConcurrentHashMap<>();
        int total = active.size();

        for (ClientSession session : active) {
            sendPing(session, completed, total, results, sender);
        }
    }

    private void pingOne(CommandSource sender, String id) {
        ClientSession session = sessions.get(id).orElse(null);
        if (session == null) {
            Report.of("Ping").error("Unknown client '" + id + "'").send(sender);
            return;
        }
        AtomicInteger completed = new AtomicInteger(0);
        ConcurrentHashMap<String, PingResult> results = new ConcurrentHashMap<>();
        sendPing(session, completed, 1, results, sender);
    }

    private void sendPing(ClientSession session, AtomicInteger completed, int total,
            ConcurrentHashMap<String, PingResult> results, CommandSource sender) {
        String id = session.id();
        String address = session.endpoint() != null ? session.endpoint().describe() : "unknown";
        try {
            outNode.send(MessageType.PING, new PingRequestContext(session, pingTimeout,
                    (success, latency) -> {
                        results.put(id, new PingResult(success ? latency : -1L, address));
                        if (completed.incrementAndGet() == total) {
                            renderPing(sender, results);
                        }
                    }));
        } catch (Exception ex) {
            results.put(id, new PingResult(-1L, address));
            if (completed.incrementAndGet() == total) {
                renderPing(sender, results);
            }
        }
    }

    private void renderPing(CommandSource sender, ConcurrentHashMap<String, PingResult> results) {
        Report report = Report.of("Ping");
        int ok = 0;
        int failed = 0;
        for (var entry : results.entrySet()) {
            PingResult res = entry.getValue();
            Status status;
            String latency;
            String quality;
            if (res.latency() < 0) {
                status = Status.ERROR;
                latency = "FAILED";
                quality = "N/A";
                failed++;
            } else if (res.latency() < 50) {
                status = Status.SUCCESS;
                latency = res.latency() + "ms";
                quality = "Excellent";
                ok++;
            } else if (res.latency() < 150) {
                status = Status.ACCENT;
                latency = res.latency() + "ms";
                quality = "Good";
                ok++;
            } else {
                status = Status.ERROR;
                latency = res.latency() + "ms";
                quality = "Poor";
                ok++;
            }
            report.listItem(entry.getKey(),
                    res.address() + " · " + latency + " · " + quality,
                    status, null);
        }
        report.summarySection()
                .summary("Responded", ok, Status.SUCCESS)
                .summary("Failed", failed, Status.ERROR);
        report.send(sender);
    }

    private record PingResult(long latency, String address) {
    }
}
