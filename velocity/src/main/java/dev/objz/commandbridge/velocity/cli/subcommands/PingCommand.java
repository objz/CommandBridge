package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.out.ctx.PingRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;

import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.cli.CliTable;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PingCommand extends AbstractCliCommand {

    private final SessionHub sessions;
    private final OutNode<Object> outNode;
    private final Duration pingTimeout;

    public PingCommand(SessionHub sessions, OutNode<Object> outNode, VelocityConfig config) {
        this.sessions = sessions;
        this.outNode = outNode;
        this.pingTimeout = Duration.ofSeconds(config.timeouts().pingTimeout());
    }

    public void execute(CommandSource sender, String clientId) {
        if (clientId == null) {
            pingAll(sender);
        } else {
            pingSingle(sender, clientId);
        }
    }

    private void pingAll(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        List<ClientSession> activeClients = getActiveClients();

        if (activeClients.isEmpty()) {
            if (ctx.isPlayer()) {
                renderChatMessage(ctx, MM.warn("No authenticated clients to ping"));
            } else {
                renderNoClientsConsole();
            }
            return;
        }

        AtomicInteger completed = new AtomicInteger(0);
        ConcurrentHashMap<String, PingResult> results = new ConcurrentHashMap<>();
        int total = activeClients.size();

        for (ClientSession session : activeClients) {
            String id = session.id();
            String address = session.endpoint() != null ? session.endpoint().describe() : "unknown";

            try {
                outNode.send(
                        MessageType.PING,
                        new PingRequestContext(session, pingTimeout,
                                (success, latency) -> {
                                    results.put(id, new PingResult(
                                            success ? latency : -1L,
                                            address));
                                    if (completed.incrementAndGet() == total) {
                                        displayResults(sender, results);
                                    }
                                }));
            } catch (Exception ex) {
                results.put(id, new PingResult(-1L, address));
                if (completed.incrementAndGet() == total) {
                    displayResults(sender, results);
                }
            }
        }
    }

    private void pingSingle(CommandSource sender, String clientId) {
        ClientSession session = findClient(clientId);
        RenderContext ctx = new RenderContext(sender);

        if (session == null) {
            if (ctx.isPlayer()) {
                renderChatMessage(ctx, MM.error("Client not found: " + clientId));
            } else {
                renderClientNotFoundConsole(clientId);
            }
            return;
        }

        String address = session.endpoint() != null ? session.endpoint().describe() : "unknown";

        try {
            outNode.send(
                    MessageType.PING,
                    new PingRequestContext(session, pingTimeout,
                            (success, latency) -> {
                                ConcurrentHashMap<String, PingResult> results = new ConcurrentHashMap<>();
                                results.put(clientId, new PingResult(success ? latency : -1L, address));
                                displayResults(sender, results);
                            }));
        } catch (Exception ex) {
            ConcurrentHashMap<String, PingResult> results = new ConcurrentHashMap<>();
            results.put(clientId, new PingResult(-1L, address));
            displayResults(sender, results);
        }
    }

    private void displayResults(CommandSource sender, ConcurrentHashMap<String, PingResult> results) {
        RenderContext ctx = new RenderContext(sender);
        
        if (ctx.isPlayer()) {
            renderChatResults(ctx, results);
        } else {
            renderConsoleResults(results);
        }
    }

    private void renderChatResults(RenderContext ctx, ConcurrentHashMap<String, PingResult> results) {
        List<Component> lines = new ArrayList<>();
        for (var entry : results.entrySet()) {
            PingResult res = entry.getValue();
            long latency = res.latency;

            String clientId = entry.getKey();
            String address = res.address;
            String latStr;
            String quality;
            String color;

            if (latency >= 0) {
                latStr = latency + "ms";
                if (latency < 50) {
                    quality = "Excellent";
                    color = Theme.C_SUCCESS;
                } else if (latency < 150) {
                    quality = "Good";
                    color = Theme.C_ACCENT;
                } else {
                    quality = "Poor";
                    color = Theme.C_ERROR;
                }
            } else {
                latStr = "FAILED";
                quality = "N/A";
                color = Theme.C_ERROR;
            }

            Component header = MM.parse("<" + Theme.C_ACCENT + ">•</" + Theme.C_ACCENT + "> ")
                    .append(MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" + clientId + "</bold></gradient>"));
            Component addrLine = MM.parse("<" + Theme.C_MUTED + ">  Address:</" + Theme.C_MUTED + "> <white>" + address + "</white>");
            Component latencyLine = MM.parse("<" + Theme.C_MUTED + ">  Latency:</" + Theme.C_MUTED + "> <" + color + ">" + latStr + "</" + color + ">");
            Component qualityLine = MM.parse("<" + Theme.C_MUTED + ">  Quality:</" + Theme.C_MUTED + "> <" + color + ">" + quality + "</" + color + ">");

            lines.add(header);
            lines.add(addrLine);
            lines.add(latencyLine);
            lines.add(qualityLine);
            lines.add(Component.empty());
        }
        if (!lines.isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        Component hint = MM.parse("<" + Theme.C_MUTED + ">Latency reflects round trip time</" + Theme.C_MUTED + ">");

        ChatFrame frame = new ChatFrame("Ping")
                .hint(hint);
        frame.lines(lines);
        frame.send(ctx.source());
    }

    private void renderChatMessage(RenderContext ctx, Component line) {
        ChatFrame frame = new ChatFrame("Ping");
        frame.line(line);
        frame.send(ctx.source());
    }

    private void renderConsoleResults(ConcurrentHashMap<String, PingResult> results) {
        CliOutput output = cli("Ping");
        CliTable table = new CliTable()
                .width(output.width())
                .addColumn("Client", CliTable.Align.LEFT, 1, 10)
                .addColumn("Address", CliTable.Align.LEFT, 6, 18)
                .addColumn("Latency", CliTable.Align.RIGHT, 1, 8)
                .addColumn("Quality", CliTable.Align.LEFT, 1, 10);

        for (var entry : results.entrySet()) {
            PingResult res = entry.getValue();
            long latency = res.latency;

            String clientId = entry.getKey();
            String address = res.address;
            String latStr;
            String quality;
            String color;

            if (latency >= 0) {
                latStr = latency + "ms";
                if (latency < 50) {
                    quality = "Excellent";
                    color = Theme.ANSI_SUCCESS;
                } else if (latency < 150) {
                    quality = "Good";
                    color = Theme.ANSI_ACCENT;
                } else {
                    quality = "Poor";
                    color = Theme.ANSI_ERROR;
                }
            } else {
                latStr = "FAILED";
                quality = "N/A";
                color = Theme.ANSI_ERROR;
            }

            String coloredLatency = color + latStr + Theme.ANSI_RESET;
            String coloredQuality = color + quality + Theme.ANSI_RESET;
            table.addRow(clientId, address, coloredLatency, coloredQuality);
        }

        output.appendRaw(table.render());
        log(output);
    }

    private void renderNoClientsConsole() {
        CliOutput output = cli("Ping");
        output.warn("No authenticated clients to ping");
        log(output);
    }

    private void renderClientNotFoundConsole(String clientId) {
        CliOutput output = cli("Ping");
        output.error("Client not found: " + clientId);
        log(output);
    }

    

    private List<ClientSession> getActiveClients() {
        List<ClientSession> activeClients = new ArrayList<>();
        for (ClientSession session : sessions) {
            if (session.status() == AuthStatus.AUTH_OK && session.endpoint() != null
                    && session.endpoint().isOpen()) {
                activeClients.add(session);
            }
        }
        return activeClients;
    }
    
    private ClientSession findClient(String clientId) {
         for (ClientSession session : sessions) {
            if (session.id() != null && session.id().equalsIgnoreCase(clientId)) {
                return session;
            }
        }
        return null;
    }

    private static class PingResult {
        final long latency;
        final String address;

        PingResult(long latency, String address) {
            this.latency = latency;
            this.address = address;
        }
    }
}
