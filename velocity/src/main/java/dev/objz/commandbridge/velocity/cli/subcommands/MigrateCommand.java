package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.migration.MigrationResult;
import dev.objz.commandbridge.scripting.migration.ScriptMigrator;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;
import dev.objz.commandbridge.velocity.ui.chat.ChatLayout;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.cli.CliTable;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MigrateCommand extends AbstractCliCommand {

    private final Path scriptsDir;

    public MigrateCommand(Path scriptsDir) {
        this.scriptsDir = scriptsDir;
    }

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        long startNs = System.nanoTime();

        ScriptMigrator migrator = new ScriptMigrator(4);

        List<Path> yamlFiles = new ArrayList<>();
        try (Stream<Path> files = Files.list(scriptsDir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                if (isYaml(p)) {
                    yamlFiles.add(p);
                }
            }
        } catch (IOException e) {
            Log.error(e, "Failed to list scripts at '{}'", scriptsDir);
            sendError(ctx, "Failed to read scripts directory", startNs);
            return;
        }

        if (yamlFiles.isEmpty()) {
            sendEmpty(ctx, startNs);
            return;
        }

        List<FileResult> results = new ArrayList<>();
        for (Path file : yamlFiles) {
            String filename = file.getFileName().toString();
            String yaml;
            try {
                yaml = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                results.add(new FileResult(filename, FileStatus.ERROR, -1, -1, "Read error: " + e.getMessage()));
                continue;
            }

            MigrationResult result = migrator.migrate(yaml);

            if (result.skipped()) {
                results.add(
                        new FileResult(filename, FileStatus.SKIPPED, result.fromVersion(), result.toVersion(), null));
                continue;
            }

            if (!result.ok()) {
                results.add(new FileResult(filename, FileStatus.ERROR, result.fromVersion(), result.toVersion(),
                        result.error()));
                continue;
            }

            try {
                Files.writeString(file, result.yaml(), StandardCharsets.UTF_8);
                results.add(new FileResult(filename, FileStatus.MIGRATED,
                        result.fromVersion(), result.toVersion(), null));
            } catch (IOException e) {
                results.add(new FileResult(filename, FileStatus.ERROR,
                        result.fromVersion(), result.toVersion(), "Write error: " + e.getMessage()));
            }
        }

        if (ctx.isPlayer()) {
            renderChat(ctx, results, migrator.currentVersion());
        } else {
            renderConsole(results, migrator.currentVersion(), startNs);
        }
    }

    // ── Console rendering ──────────────────────────────────────────────

    private void renderConsole(List<FileResult> results, int targetVersion, long startNs) {
        int migrated = 0;
        int skipped = 0;
        int errors = 0;
        for (FileResult r : results) {
            switch (r.status) {
                case MIGRATED -> migrated++;
                case SKIPPED -> skipped++;
                case ERROR -> errors++;
            }
        }

        CliOutput output = cli("Migrate");

        if (errors > 0) {
            output.warn("Migration completed with " + errors + " error(s)");
        } else if (migrated > 0) {
            output.success("Migration completed");
        } else {
            output.muted("All scripts are already at version " + targetVersion);
        }

        output.blankLine();
        output.accent("Summary");
        output.appendRaw(buildSummaryTable(output.width(), results.size(), migrated, skipped, errors).render());
        output.blankLine();

        if (migrated > 0 || errors > 0) {
            output.accent("Results");
            output.appendRaw(buildResultsTable(output.width(), results).render());
        }

        output.muted("Completed in " + elapsedMs(startNs) + "ms");
        log(output);
    }

    private CliTable buildSummaryTable(int width, int total, int migrated, int skipped, int errors) {
        CliTable table = new CliTable()
                .width(width)
                .addColumn("Total", CliTable.Align.RIGHT, 1, 5)
                .addColumn("Migrated", CliTable.Align.RIGHT, 1, 8)
                .addColumn("Skipped", CliTable.Align.RIGHT, 1, 7)
                .addColumn("Errors", CliTable.Align.RIGHT, 1, 6);
        table.addRow(
                Theme.ANSI_ACCENT + total + Theme.ANSI_RESET,
                Theme.ANSI_SUCCESS + migrated + Theme.ANSI_RESET,
                Theme.ANSI_MUTED + skipped + Theme.ANSI_RESET,
                Theme.ANSI_ERROR + errors + Theme.ANSI_RESET);
        return table;
    }

    private CliTable buildResultsTable(int width, List<FileResult> results) {
        CliTable table = new CliTable()
                .width(width)
                .addColumn("Script", CliTable.Align.LEFT, 3, 12)
                .addColumn("Status", CliTable.Align.LEFT, 1, 8)
                .addColumn("Version", CliTable.Align.LEFT, 1, 9)
                .addColumn("Detail", CliTable.Align.LEFT, 4, 12);

        for (FileResult r : results) {
            if (r.status == FileStatus.SKIPPED) {
                continue;
            }

            String status;
            String color;
            switch (r.status) {
                case MIGRATED -> {
                    status = "OK";
                    color = Theme.ANSI_SUCCESS;
                }
                case ERROR -> {
                    status = "ERROR";
                    color = Theme.ANSI_ERROR;
                }
                default -> {
                    status = "UNKNOWN";
                    color = Theme.ANSI_MUTED;
                }
            }

            String version = r.from > 0 && r.to > 0
                    ? r.from + " -> " + r.to
                    : "?";
            String detail = r.error != null ? r.error : "";

            table.addRow(
                    r.filename,
                    color + status + Theme.ANSI_RESET,
                    version,
                    detail);
        }

        return table;
    }

    // ── Chat rendering ─────────────────────────────────────────────────

    private void renderChat(RenderContext ctx, List<FileResult> results, int targetVersion) {
        int migrated = 0;
        int skipped = 0;
        int errors = 0;
        for (FileResult r : results) {
            switch (r.status) {
                case MIGRATED -> migrated++;
                case SKIPPED -> skipped++;
                case ERROR -> errors++;
            }
        }

        List<Component> lines = new ArrayList<>();

        if (errors > 0) {
            lines.add(MM.parse("<" + Theme.C_WARN + "><bold>Migration completed with "
                    + errors + " error(s)</bold></" + Theme.C_WARN + ">"));
        } else if (migrated > 0) {
            lines.add(MM.parse("<" + Theme.C_SUCCESS + "><bold>Migration completed</bold></" + Theme.C_SUCCESS + ">"));
        } else {
            lines.add(MM.parse("<" + Theme.C_MUTED + ">All scripts are already at version "
                    + targetVersion + "</" + Theme.C_MUTED + ">"));
        }

        lines.add(Component.empty());
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Total:</" + Theme.C_MUTED + "> <" + Theme.C_ACCENT + ">"
                + results.size() + "</" + Theme.C_ACCENT + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Migrated:</" + Theme.C_MUTED + "> <" + Theme.C_SUCCESS + ">"
                + migrated + "</" + Theme.C_SUCCESS + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Skipped:</" + Theme.C_MUTED + "> <" + Theme.C_MUTED + ">"
                + skipped + "</" + Theme.C_MUTED + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Errors:</" + Theme.C_MUTED + "> <" + Theme.C_ERROR + ">"
                + errors + "</" + Theme.C_ERROR + ">"));

        if (migrated > 0 || errors > 0) {
            lines.add(Component.empty());

            for (FileResult r : results) {
                if (r.status == FileStatus.SKIPPED) {
                    continue;
                }

                String statusText;
                String color;
                switch (r.status) {
                    case MIGRATED -> {
                        statusText = "OK";
                        color = Theme.C_SUCCESS;
                    }
                    case ERROR -> {
                        statusText = "ERROR";
                        color = Theme.C_ERROR;
                    }
                    default -> {
                        statusText = "UNKNOWN";
                        color = Theme.C_MUTED;
                    }
                }

                String version = r.from > 0 && r.to > 0
                        ? "v" + r.from + " -> v" + r.to
                        : "";

                Component header = MM
                        .parse("<" + Theme.C_ACCENT + ">" + Theme.SYMBOL_BULLET + "</" + Theme.C_ACCENT + "> ")
                        .append(MM.parse("<white>" + r.filename + "</white>"))
                        .append(MM.parse(" <" + color + ">" + statusText + "</" + color + ">"));
                lines.add(header);

                if (!version.isEmpty()) {
                    lines.add(MM.parse("<" + Theme.C_MUTED + ">  " + version + "</" + Theme.C_MUTED + ">"));
                }
                if (r.error != null && !r.error.isBlank()) {
                    lines.add(MM.parse("<" + Theme.C_ERROR + ">  " + r.error + "</" + Theme.C_ERROR + ">"));
                }
            }
        }

        if (migrated > 0) {
            lines.add(Component.empty());
            Component reloadHint = MM.parse("<" + Theme.C_MUTED + ">Run </" + Theme.C_MUTED + ">")
                    .append(MM.cmd("/cb reload"))
                    .append(MM.parse("<" + Theme.C_MUTED + "> to apply changes</" + Theme.C_MUTED + ">"));
            lines.add(reloadHint);
        }

        int width = ChatLayout.titleWidth("Migrate");
        for (Component line : lines) {
            width = Math.max(width, ChatLayout.visibleLength(line));
        }
        width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);

        ChatFrame frame = new ChatFrame("Migrate").width(width);
        frame.lines(lines);
        frame.send(ctx.source());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void sendError(RenderContext ctx, String message, long startNs) {
        if (ctx.isPlayer()) {
            Component err = MM.error(message);
            int width = Math.max(ChatLayout.titleWidth("Migrate"), ChatLayout.visibleLength(err));
            width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);
            ChatFrame frame = new ChatFrame("Migrate").width(width);
            frame.line(err);
            frame.send(ctx.source());
        } else {
            CliOutput output = cli("Migrate");
            output.error(message);
            output.muted("Completed in " + elapsedMs(startNs) + "ms");
            log(output);
        }
    }

    private void sendEmpty(RenderContext ctx, long startNs) {
        if (ctx.isPlayer()) {
            Component msg = MM.muted("No scripts found in scripts directory");
            int width = Math.max(ChatLayout.titleWidth("Migrate"), ChatLayout.visibleLength(msg));
            width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);
            ChatFrame frame = new ChatFrame("Migrate").width(width);
            frame.line(msg);
            frame.send(ctx.source());
        } else {
            CliOutput output = cli("Migrate");
            output.muted("No scripts found in scripts directory");
            output.muted("Completed in " + elapsedMs(startNs) + "ms");
            log(output);
        }
    }

    private long elapsedMs(long startNs) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
    }

    private static boolean isYaml(Path p) {
        if (!Files.isRegularFile(p))
            return false;
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".yml") || n.endsWith(".yaml");
    }

    private enum FileStatus {
        MIGRATED, SKIPPED, ERROR
    }

    private static class FileResult {
        final String filename;
        final FileStatus status;
        final int from;
        final int to;
        final String error;

        FileResult(String filename, FileStatus status, int from, int to, String error) {
            this.filename = filename;
            this.status = status;
            this.from = from;
            this.to = to;
            this.error = error;
        }
    }
}
