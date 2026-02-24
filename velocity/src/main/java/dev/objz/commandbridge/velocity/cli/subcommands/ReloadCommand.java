package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.net.out.ctx.RegistrationRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;
import dev.objz.commandbridge.velocity.ui.chat.ChatLayout;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.cli.CliTable;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

public class ReloadCommand extends AbstractCliCommand {

    private final ConfigManager configManager;
    private final ScriptManager scriptManager;
    private final RegistrationManager registrationManager;
    private final SessionHub sessionHub;
    private final OutNode<Object> outNode;

    public ReloadCommand(ConfigManager configManager, ScriptManager scriptManager,
            RegistrationManager registrationManager, SessionHub sessionHub,
            OutNode<Object> outNode) {
        this.configManager = configManager;
        this.scriptManager = scriptManager;
        this.registrationManager = registrationManager;
        this.sessionHub = sessionHub;
        this.outNode = outNode;
    }

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        long startNs = System.nanoTime();

        try {
            boolean configOk = configManager.reload(VelocityConfig.class);
            var cfg = configManager.current(VelocityConfig.class);
            if (!configOk || cfg == null) {
                sendError(ctx, "Failed to reload config", "Check console for details", startNs);
                return;
            }
            Log.setDebug(cfg.debug());
            boolean debugEnabled = cfg.debug();

            scriptManager.loadAll(false);

            int enabled = scriptManager.enabled().size();
            int disabled = scriptManager.disabled().size();
            int loaded = scriptManager.loaded().size();
            int errors = (int) scriptManager.errors();

            if (errors > 0) {
                sendScriptErrors(ctx, loaded, enabled, disabled, errors, debugEnabled, startNs);
                return;
            }

            registrationManager.load(scriptManager.enabled());

            List<ClientSession> activeClients = getActiveClients();
            int clientsWithScripts = (int) activeClients.stream().filter(
                    s -> !registrationManager.getScriptsForSession(s).isEmpty())
                    .count();

            Duration registerTimeout = Duration.ofSeconds(cfg.timeouts().registerTimeout());

            if (clientsWithScripts == 0) {
                sendSuccess(ctx, loaded, enabled, disabled, errors, startNs);
                return;
            }
            AtomicInteger completed = new AtomicInteger(0);
            ConcurrentHashMap<String, ReloadResult> results = new ConcurrentHashMap<>();
            final int totalExpected = clientsWithScripts;
            final var sentTo = new ConcurrentHashMap<String, Boolean>();

            for (ClientSession session : activeClients) {
                String clientId = session.id();
                String address = session.endpoint() != null ? session.endpoint().describe() : "unknown";

                Set<dev.objz.commandbridge.scripting.model.Script> scripts = registrationManager
                        .getScriptsForSession(session);

                if (scripts == null || scripts.isEmpty()) {
                    continue;
                }

                sentTo.put(clientId, true);
                try {
                    outNode.send(MessageType.REGISTER_COMMANDS,
                            new RegistrationRequestContext(session, scripts,
                                    registerTimeout,
                                    (success) -> {
                                        ReloadStatus status = success
                                                ? ReloadStatus.SUCCESS
                                                : ReloadStatus.FAILED;
                                        results.put(clientId,
                                                new ReloadResult(status,
                                                        address,
                                                        null));
                                if (completed.incrementAndGet() == totalExpected) {
                                    displayResults(sender, results, totalExpected, loaded, enabled, disabled, errors,
                                        startNs);
                                }
                                    }));
                } catch (Exception ex) {
                    results.put(clientId, new ReloadResult(ReloadStatus.FAILED, address,
                            ex.getMessage()));
                    if (completed.incrementAndGet() == totalExpected) {
                        displayResults(sender, results, totalExpected, loaded, enabled, disabled, errors,
                                startNs);
                    }
                }
            }

            if (totalExpected > 0) {
                new Thread(() -> {
                    try {
                        Thread.sleep(registerTimeout.toMillis() + 1000);
                        if (completed.get() < totalExpected) {
                            for (ClientSession session : activeClients) {
                                String clientId = session.id();
                                if (sentTo.containsKey(clientId)
                                        && !results.containsKey(clientId)) {
                                    String address = session.endpoint() != null
                                            ? session.endpoint().describe()
                                            : "unknown";
                                    results.put(clientId,
                                            new ReloadResult(
                                                    ReloadStatus.TIMEOUT,
                                                    address,
                                                    "Registration timeout after "
                                                            + registerTimeout
                                                                    .toSeconds()
                                                            + "s"));
                                }
                            }
                            if (completed.get() < totalExpected) {
                                completed.set(totalExpected);
                                displayResults(sender, results, totalExpected, loaded, enabled, disabled, errors,
                                        startNs);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        } catch (Exception e) {
            Log.error("Reload failed: {}", e.getMessage());
            sendError(ctx, "Reload failed: " + e.getMessage(), "Check console for details", startNs);
        }
    }

    private void sendSuccess(RenderContext ctx, int loaded, int enabled, int disabled, int errors,
            long startNs) {
        if (ctx.isPlayer()) {
            renderChatSuccess(ctx, loaded, enabled, disabled);
            return;
        }

        CliOutput output = cli("Reload");
        output.success("Config and scripts reloaded");
        output.blankLine();
        output.appendRaw(buildScriptsTable(output.width(), loaded, enabled, disabled, errors).render());
        output.muted("Completed in " + elapsedMs(startNs) + "ms");
        log(output);
    }

    private void sendScriptErrors(RenderContext ctx, int loaded, int enabled, int disabled, int errors,
            boolean debugEnabled, long startNs) {
        if (ctx.isPlayer()) {
            renderChatError(ctx, "Script loading failed with " + errors + " error(s)", "Check console for details");
            return;
        }

        CliOutput output = cli("Reload");
        output.error("Script loading failed with " + errors + " error(s)");
        output.blankLine();
        output.accent("Scripts");
        output.appendRaw(buildScriptsTable(output.width(), loaded, enabled, disabled, errors).render());
        output.blankLine();
        output.muted("Debug: " + (debugEnabled ? "ENABLED" : "DISABLED"));
        output.warn("Reload aborted due to script errors");
        output.muted("Completed in " + elapsedMs(startNs) + "ms");
        log(output);
    }

    private void sendError(RenderContext ctx, String error, String details, long startNs) {
        if (ctx.isPlayer()) {
            renderChatError(ctx, error, details);
            return;
        }

        CliOutput output = cli("Reload");
        output.error(error);
        output.muted(details);
        output.muted("Completed in " + elapsedMs(startNs) + "ms");
        log(output);
    }

    private void renderChatSuccess(RenderContext ctx, int loaded, int enabled, int disabled) {
        List<Component> lines = new ArrayList<>();
        Component success = MM.parse("<" + Theme.C_SUCCESS + "><bold>Config and scripts reloaded</bold></" + Theme.C_SUCCESS + ">");
        lines.add(success);
        lines.add(Component.empty());
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Loaded:</" + Theme.C_MUTED + "> <" + Theme.C_ACCENT + ">" + loaded + "</" + Theme.C_ACCENT + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Enabled:</" + Theme.C_MUTED + "> <" + Theme.C_SUCCESS + ">" + enabled + "</" + Theme.C_SUCCESS + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Disabled:</" + Theme.C_MUTED + "> <" + Theme.C_WARN + ">" + disabled + "</" + Theme.C_WARN + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Errors:</" + Theme.C_MUTED + "> <" + Theme.C_ERROR + ">0</" + Theme.C_ERROR + ">"));
        lines.add(Component.empty());
        Component action = MM.parse("<" + Theme.C_ACCENT + "><bold>View scripts</bold></" + Theme.C_ACCENT + ">")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/cb scripts"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        MM.parse("<" + Theme.C_MUTED + ">Open script list</" + Theme.C_MUTED + ">")));
        lines.add(action);

        int width = ChatLayout.titleWidth("Reload");
        for (Component line : lines) {
            width = Math.max(width, ChatLayout.visibleLength(line));
        }
        width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);

        ChatFrame frame = new ChatFrame("Reload")
                .width(width);
        frame.lines(lines);
        frame.send(ctx.source());
    }

    private void renderChatError(RenderContext ctx, String error, String details) {
        Component err = MM.error(error);
        Component detail = MM.muted(details);
        int width = Math.max(ChatLayout.titleWidth("Reload"), ChatLayout.visibleLength(err));
        width = Math.max(width, ChatLayout.visibleLength(detail));
        width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);
        ChatFrame frame = new ChatFrame("Reload").width(width);
        frame.line(err);
        frame.line(detail);
        frame.send(ctx.source());
    }

    private List<ClientSession> getActiveClients() {
        List<ClientSession> activeClients = new ArrayList<>();
        for (ClientSession session : sessionHub) {
            if (session.status() == AuthStatus.AUTH_OK && session.endpoint() != null
                    && session.endpoint().isOpen()) {
                activeClients.add(session);
            }
        }
        return activeClients;
    }

    private void displayResults(CommandSource sender,
            ConcurrentHashMap<String, ReloadResult> results,
            int expected, int loaded, int enabled, int disabled, int errors,
            long startNs) {
        RenderContext ctx = new RenderContext(sender);

        if (ctx.isPlayer()) {
            displayChatResults(ctx, results, loaded, enabled, disabled);
        } else {
            displayConsoleResults(results, expected, loaded, enabled, disabled, errors, startNs);
        }
    }

    private void displayChatResults(RenderContext ctx,
            ConcurrentHashMap<String, ReloadResult> results,
            int loaded, int enabled, int disabled) {
        int successful = 0;
        int failed = 0;
        int timeout = 0;

        List<ReloadEntry> entries = new ArrayList<>();
        for (var entry : results.entrySet()) {
            ReloadResult result = entry.getValue();
            entries.add(new ReloadEntry(entry.getKey(), result.status, result.address, result.errorMessage));
            switch (result.status) {
                case SUCCESS -> successful++;
                case FAILED -> failed++;
                case TIMEOUT -> timeout++;
            }
        }

        entries.sort((a, b) -> {
            if (a.status != b.status) {
                return a.status.ordinal() - b.status.ordinal();
            }
            return a.id.compareTo(b.id);
        });

        List<Component> lines = new ArrayList<>();
        Component success = MM.parse("<" + Theme.C_SUCCESS + "><bold>Config and scripts reloaded</bold></" + Theme.C_SUCCESS + ">");
        lines.add(success);
        lines.add(Component.empty());
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Loaded:</" + Theme.C_MUTED + "> <" + Theme.C_ACCENT + ">" + loaded + "</" + Theme.C_ACCENT + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Enabled:</" + Theme.C_MUTED + "> <" + Theme.C_SUCCESS + ">" + enabled + "</" + Theme.C_SUCCESS + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Disabled:</" + Theme.C_MUTED + "> <" + Theme.C_WARN + ">" + disabled + "</" + Theme.C_WARN + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Errors:</" + Theme.C_MUTED + "> <" + Theme.C_ERROR + ">0</" + Theme.C_ERROR + ">"));
        lines.add(Component.empty());
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Responses:</" + Theme.C_MUTED + "> <" + Theme.C_ACCENT + ">" + results.size() + "</" + Theme.C_ACCENT + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Successful:</" + Theme.C_MUTED + "> <" + Theme.C_SUCCESS + ">" + successful + "</" + Theme.C_SUCCESS + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Failed:</" + Theme.C_MUTED + "> <" + Theme.C_ERROR + ">" + failed + "</" + Theme.C_ERROR + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Timeout:</" + Theme.C_MUTED + "> <" + Theme.C_WARN + ">" + timeout + "</" + Theme.C_WARN + ">"));
        lines.add(Component.empty());

        int idx = 0;
        for (ReloadEntry entry : entries) {
            String status;
            String color;
            switch (entry.status) {
                case SUCCESS -> { status = "OK"; color = Theme.C_SUCCESS; }
                case FAILED -> { status = "FAILED"; color = Theme.C_ERROR; }
                case TIMEOUT -> { status = "TIMEOUT"; color = Theme.C_WARN; }
                default -> { status = "UNKNOWN"; color = Theme.C_MUTED; }
            }
            Component header = MM.parse("<" + Theme.C_ACCENT + ">•</" + Theme.C_ACCENT + "> ")
                    .append(MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" + entry.id + "</bold></gradient>"))
                    .append(MM.parse(" <" + color + ">" + status + "</" + color + ">"));
            Component address = MM.parse("<" + Theme.C_MUTED + ">  Address:</" + Theme.C_MUTED + "> <white>" + entry.address + "</white>");
            lines.add(header);
            lines.add(address);
            if (entry.errorMessage != null && !entry.errorMessage.isBlank()) {
                lines.add(MM.parse("<" + Theme.C_MUTED + ">  Detail:</" + Theme.C_MUTED + "> <white>" + entry.errorMessage + "</white>"));
            }
            if (idx < entries.size() - 1) {
                lines.add(Component.empty());
            }
            idx++;
        }

        int width = ChatLayout.titleWidth("Reload");
        for (Component line : lines) {
            width = Math.max(width, ChatLayout.visibleLength(line));
        }
        width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);

        ChatFrame frame = new ChatFrame("Reload")
                .width(width);
        frame.lines(lines);
        frame.send(ctx.source());
    }

    private void displayConsoleResults(ConcurrentHashMap<String, ReloadResult> results,
            int expected, int loaded, int enabled, int disabled, int errors, long startNs) {
        int successful = 0;
        int failed = 0;
        int timeout = 0;

        List<ReloadEntry> entries = new ArrayList<>();
        for (var entry : results.entrySet()) {
            ReloadResult result = entry.getValue();
            entries.add(new ReloadEntry(entry.getKey(), result.status, result.address,
                    result.errorMessage));

            switch (result.status) {
                case SUCCESS -> successful++;
                case FAILED -> failed++;
                case TIMEOUT -> timeout++;
            }
        }

        entries.sort((a, b) -> {
            if (a.status != b.status) {
                return a.status.ordinal() - b.status.ordinal();
            }
            return a.id.compareTo(b.id);
        });

        CliOutput output = cli("Reload");
        output.success("Config and scripts reloaded");
        output.blankLine();
        output.appendRaw(buildScriptsTable(output.width(), loaded, enabled, disabled, errors).render());
        output.blankLine();
        output.accent("Registration Summary");
        output.appendRaw(buildRegistrationSummaryTable(output.width(), expected, results.size(),
                successful, failed, timeout).render());
        output.blankLine();
        output.accent("Client Results");
        output.appendRaw(buildRegistrationResultsTable(output.width(), entries).render());

        if (failed > 0 || timeout > 0) {
            output.blankLine();
            output.muted("Check console for detailed error messages");
        }
        output.muted("Completed in " + elapsedMs(startNs) + "ms");

        log(output);
    }

    private CliTable buildScriptsTable(int width, int loaded, int enabled, int disabled, int errors) {
        CliTable table = new CliTable()
                .width(width)
                .addColumn("Loaded", CliTable.Align.RIGHT, 1, 6)
                .addColumn("Enabled", CliTable.Align.RIGHT, 1, 7)
                .addColumn("Disabled", CliTable.Align.RIGHT, 1, 8)
                .addColumn("Errors", CliTable.Align.RIGHT, 1, 6);
        String loadedVal = Theme.ANSI_ACCENT + loaded + Theme.ANSI_RESET;
        String enabledVal = Theme.ANSI_SUCCESS + enabled + Theme.ANSI_RESET;
        String disabledVal = Theme.ANSI_WARN + disabled + Theme.ANSI_RESET;
        String errorsVal = Theme.ANSI_ERROR + errors + Theme.ANSI_RESET;
        table.addRow(loadedVal, enabledVal, disabledVal, errorsVal);
        return table;
    }

    private CliTable buildRegistrationSummaryTable(int width, int expected, int responses,
            int successful, int failed, int timeout) {
        CliTable table = new CliTable()
                .width(width)
                .addColumn("Responses", CliTable.Align.RIGHT, 1, 9)
                .addColumn("Successful", CliTable.Align.RIGHT, 1, 9)
                .addColumn("Failed", CliTable.Align.RIGHT, 1, 7)
                .addColumn("Timeout", CliTable.Align.RIGHT, 1, 8);
        String responseVal = Theme.ANSI_ACCENT + responses + "/" + expected + Theme.ANSI_RESET;
        String successVal = Theme.ANSI_SUCCESS + successful + Theme.ANSI_RESET;
        String failedVal = Theme.ANSI_ERROR + failed + Theme.ANSI_RESET;
        String timeoutVal = Theme.ANSI_WARN + timeout + Theme.ANSI_RESET;
        table.addRow(responseVal, successVal, failedVal, timeoutVal);
        return table;
    }

    private CliTable buildRegistrationResultsTable(int width, List<ReloadEntry> entries) {
        CliTable table = new CliTable()
                .width(width)
                .addColumn("Client", CliTable.Align.LEFT, 2, 10)
                .addColumn("Address", CliTable.Align.LEFT, 4, 18)
                .addColumn("Status", CliTable.Align.LEFT, 1, 8)
                .addColumn("Detail", CliTable.Align.LEFT, 5, 14);

        for (ReloadEntry entry : entries) {
            String status;
            String color;
            switch (entry.status) {
                case SUCCESS -> {
                    status = "OK";
                    color = Theme.ANSI_SUCCESS;
                }
                case FAILED -> {
                    status = "FAILED";
                    color = Theme.ANSI_ERROR;
                }
                case TIMEOUT -> {
                    status = "TIMEOUT";
                    color = Theme.ANSI_WARN;
                }
                default -> {
                    status = "UNKNOWN";
                    color = Theme.ANSI_MUTED;
                }
            }

            String detail = entry.errorMessage != null ? entry.errorMessage : "";
            table.addRow(entry.id, entry.address, color + status + Theme.ANSI_RESET, detail);
        }
        return table;
    }

    private long elapsedMs(long startNs) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
    }

    private enum ReloadStatus {
        SUCCESS, FAILED, TIMEOUT
    }

    private static class ReloadEntry {
        final String id;
        final ReloadStatus status;
        final String address;
        final String errorMessage;

        ReloadEntry(String id, ReloadStatus status, String address, String errorMessage) {
            this.id = id;
            this.status = status;
            this.address = address;
            this.errorMessage = errorMessage;
        }
    }

    private static class ReloadResult {
        final ReloadStatus status;
        final String address;
        final String errorMessage;

        ReloadResult(ReloadStatus status, String address, String errorMessage) {
            this.status = status;
            this.address = address;
            this.errorMessage = errorMessage;
        }
    }
}
