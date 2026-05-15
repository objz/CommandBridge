package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.dump.DumpExportResult;
import dev.objz.commandbridge.velocity.dump.DumpExporter;
import dev.objz.commandbridge.velocity.dump.RemoteDumpCollector;
import dev.objz.commandbridge.velocity.dump.SupportDumpBuilder;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.RenderContext;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public final class DumpCommand extends AbstractCliCommand {

    private final RegistrationManager registrations;
    private final SessionHub sessions;
    private final OutNode outNode;
    private final ScriptManager scripts;
    private final VelocityConfig config;
    private final DumpExporter exporter;

    public DumpCommand(
            RegistrationManager registrations,
            SessionHub sessions,
            OutNode outNode,
            ScriptManager scripts,
            VelocityConfig config,
            Path dataDir) {
        this.registrations = registrations;
        this.sessions = sessions;
        this.outNode = outNode;
        this.scripts = scripts;
        this.config = config;
        this.exporter = new DumpExporter(dataDir);
    }

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        int clientCount = sessions.size();

        try {
            Duration timeout = Duration.ofSeconds(Math.max(2, config.timeouts().pingTimeout()));
            RemoteDumpCollector collector = new RemoteDumpCollector(sessions, outNode, timeout);
            List<JsonNode> remoteSnapshots = collector.collect();
            int remoteCollected = 0;
            for (JsonNode snapshot : remoteSnapshots) {
                if (snapshot != null && snapshot.path("collected").asBoolean(false)) {
                    remoteCollected++;
                }
            }
            int remoteFailed = remoteSnapshots.size() - remoteCollected;

            SupportDumpBuilder builder = new SupportDumpBuilder(
                    registrations,
                    sessions,
                    scripts,
                    config,
                    remoteSnapshots);
            String payload = builder.buildJson();
            DumpExportResult result = exporter.export(payload);

            if (ctx.isPlayer()) {
                renderChatSuccess(ctx, clientCount, remoteCollected, remoteFailed, result);
            } else {
                renderConsoleSuccess(clientCount, remoteCollected, remoteFailed, result);
            }
        } catch (Exception ex) {
            if (ctx.isPlayer()) {
                renderChatError(ctx, ex);
            } else {
                renderConsoleError(ex);
            }
        }
    }

    private void renderChatSuccess(
            RenderContext ctx,
            int clientCount,
            int remoteCollected,
            int remoteFailed,
            DumpExportResult result) {
        String kb = String.format("%.1f KB", result.bytes() / 1024.0d);
        String snapshots = remoteCollected + " ok, " + remoteFailed + " failed";

        var report = dev.objz.commandbridge.velocity.ui.Report.of("Dump")
                .success("Dump created")
                .blank()
                .kv("Clients", String.valueOf(clientCount),
                        dev.objz.commandbridge.velocity.ui.Report.Status.ACCENT)
                .kv("Snapshots", snapshots)
                .kv("Size", kb);

        if (result.uploaded()) {
            report.link("Viewer", result.upload().url(), "Click to open");
            if (result.upload().id() != null && !result.upload().id().isBlank()) {
                report.kv("Dump ID", result.upload().id());
            }
            if (result.upload().expiresAt() != null
                    && !result.upload().expiresAt().isBlank()) {
                report.kv("Expires", result.upload().expiresAt());
            }
        } else {
            report.warn("Upload failed — use local file");
            if (result.uploadError() != null) {
                report.error(safeMessage(result.uploadError()));
            }
        }

        if (result.localPath() != null) {
            report.kv("Local copy", result.localPath().toString());
        } else if (result.localError() != null) {
            report.error("Local save failed: " + safeMessage(result.localError()));
        }

        report.sendChatOnly(ctx.source());
    }

    private void renderConsoleSuccess(int clientCount, int remoteCollected, int remoteFailed, DumpExportResult result) {
        CliOutput output = cli("Dump");
        output.line("Clients: " + clientCount);
        output.line("Remote snapshots: " + remoteCollected + " ok, " + remoteFailed + " failed");
        output.line("Dump size: " + String.format("%.1f KB", result.bytes() / 1024.0d));

        if (result.uploaded()) {
            output.success("Uploaded: " + result.upload().url());
            if (result.upload().id() != null && !result.upload().id().isBlank()) {
                output.line("Dump ID: " + result.upload().id());
            }
            if (result.upload().expiresAt() != null && !result.upload().expiresAt().isBlank()) {
                output.line("Expires: " + result.upload().expiresAt());
            }
        } else {
            output.warn("Upload failed");
            if (result.uploadError() != null) {
                output.error(safeMessage(result.uploadError()));
            }
        }

        if (result.localPath() != null) {
            output.line("Local copy: " + result.localPath());
        } else if (result.localError() != null) {
            output.error("Local save failed: " + safeMessage(result.localError()));
        }

        log(output);
    }

    private void renderChatError(RenderContext ctx, Exception ex) {
        dev.objz.commandbridge.velocity.ui.Report.of("Dump")
                .error("Failed to build dump: " + safeMessage(ex))
                .sendChatOnly(ctx.source());
    }

    private void renderConsoleError(Exception ex) {
        CliOutput output = cli("Dump");
        output.error("Failed to build dump: " + safeMessage(ex));
        log(output);
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "unknown error";
        }
        return throwable.getMessage();
    }
}
