package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.migration.MigrationResult;
import dev.objz.commandbridge.scripting.migration.YamlMigrator;
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

    private static final int CURRENT_SCRIPT_VERSION = 4;
    private static final int CURRENT_CONFIG_VERSION = 2;

    private final Path scriptsDir;
    private final Path dataDir;

    public MigrateCommand(Path scriptsDir, Path dataDir) {
        this.scriptsDir = scriptsDir;
        this.dataDir = dataDir;
    }

    public void execute(CommandSource sender) {
        RenderContext ctx = new RenderContext(sender);
        long startNs = System.nanoTime();

        YamlMigrator scriptMigrator = new YamlMigrator(CURRENT_SCRIPT_VERSION);
        YamlMigrator configMigrator = new YamlMigrator(CURRENT_CONFIG_VERSION);

        List<FileResult> scriptResults = migrateScripts(scriptMigrator);
        List<FileResult> configResults = migrateConfigs(configMigrator);

        if (ctx.isPlayer()) {
            renderChat(ctx, scriptResults, configResults, scriptMigrator, configMigrator);
        } else {
            renderConsole(scriptResults, configResults, scriptMigrator, configMigrator, startNs);
        }
    }

    // ── Script migration ───────────────────────────────────────────────

    private List<FileResult> migrateScripts(YamlMigrator migrator) {
        List<Path> yamlFiles = new ArrayList<>();
        try (Stream<Path> files = Files.list(scriptsDir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                if (isYaml(p)) {
                    yamlFiles.add(p);
                }
            }
        } catch (IOException e) {
            Log.error(e, "Failed to list scripts at '{}'", scriptsDir);
            return List.of(new FileResult("scripts/", FileStatus.ERROR, -1, -1,
                    "Failed to read scripts directory"));
        }

        List<FileResult> results = new ArrayList<>();
        for (Path file : yamlFiles) {
            results.add(migrateFile(file, migrator, YamlMigrator.SECTION_SCRIPTS, -1));
        }
        return results;
    }

    // ── Config migration ───────────────────────────────────────────────

    private List<FileResult> migrateConfigs(YamlMigrator migrator) {
        List<FileResult> results = new ArrayList<>();

        Path configFile = dataDir.resolve("config.yml");
        if (Files.exists(configFile)) {
            results.add(migrateFile(configFile, migrator, YamlMigrator.SECTION_CONFIGS, 1));
        }

        return results;
    }

    // ── Shared file migration ──────────────────────────────────────────

    private FileResult migrateFile(Path file, YamlMigrator migrator, String section,
            int defaultVersion) {
        String filename = file.getFileName().toString();
        String yaml;
        try {
            yaml = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new FileResult(filename, FileStatus.ERROR, -1, -1,
                    "Read error: " + e.getMessage());
        }

        MigrationResult result = migrator.migrate(yaml, section, defaultVersion);

        if (result.skipped()) {
            return new FileResult(filename, FileStatus.SKIPPED,
                    result.fromVersion(), result.toVersion(), null);
        }

        if (!result.ok()) {
            return new FileResult(filename, FileStatus.ERROR,
                    result.fromVersion(), result.toVersion(), result.error());
        }

        try {
            Files.writeString(file, result.yaml(), StandardCharsets.UTF_8);
            return new FileResult(filename, FileStatus.MIGRATED,
                    result.fromVersion(), result.toVersion(), null);
        } catch (IOException e) {
            return new FileResult(filename, FileStatus.ERROR,
                    result.fromVersion(), result.toVersion(),
                    "Write error: " + e.getMessage());
        }
    }

    // ── Console rendering ──────────────────────────────────────────────

    private void renderConsole(List<FileResult> scriptResults, List<FileResult> configResults,
            YamlMigrator scriptMigrator, YamlMigrator configMigrator, long startNs) {

        int totalMigrated = countStatus(scriptResults, FileStatus.MIGRATED)
                + countStatus(configResults, FileStatus.MIGRATED);
        int totalErrors = countStatus(scriptResults, FileStatus.ERROR)
                + countStatus(configResults, FileStatus.ERROR);

        CliOutput output = cli("Migrate");

        if (totalErrors > 0) {
            output.warn("Migration completed with " + totalErrors + " error(s)");
        } else if (totalMigrated > 0) {
            output.success("Migration completed");
        } else {
            output.muted("Everything is already up to date");
        }

        // Scripts section
        renderConsoleSection(output, "Scripts", scriptResults, scriptMigrator.currentVersion());

        // Configs section
        renderConsoleSection(output, "Configs", configResults, configMigrator.currentVersion());

        output.muted("Completed in " + elapsedMs(startNs) + "ms");
        log(output);
    }

    private void renderConsoleSection(CliOutput output, String title, List<FileResult> results,
            int targetVersion) {
        int migrated = countStatus(results, FileStatus.MIGRATED);
        int skipped = countStatus(results, FileStatus.SKIPPED);
        int errors = countStatus(results, FileStatus.ERROR);

        output.blankLine();
        output.accent(title);
        output.appendRaw(buildSummaryTable(output.width(), results.size(),
                migrated, skipped, errors).render());

        if (migrated > 0 || errors > 0) {
            output.appendRaw(buildResultsTable(output.width(), results).render());
        }
    }

    private CliTable buildSummaryTable(int width, int total, int migrated, int skipped,
            int errors) {
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
                .addColumn("File", CliTable.Align.LEFT, 3, 12)
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

    private void renderChat(RenderContext ctx, List<FileResult> scriptResults,
            List<FileResult> configResults, YamlMigrator scriptMigrator,
            YamlMigrator configMigrator) {

        int totalMigrated = countStatus(scriptResults, FileStatus.MIGRATED)
                + countStatus(configResults, FileStatus.MIGRATED);
        int totalErrors = countStatus(scriptResults, FileStatus.ERROR)
                + countStatus(configResults, FileStatus.ERROR);

        List<Component> lines = new ArrayList<>();

        if (totalErrors > 0) {
            lines.add(MM.parse("<" + Theme.C_WARN + "><bold>Migration completed with "
                    + totalErrors + " error(s)</bold></" + Theme.C_WARN + ">"));
        } else if (totalMigrated > 0) {
            lines.add(MM.parse("<" + Theme.C_SUCCESS + "><bold>Migration completed</bold></"
                    + Theme.C_SUCCESS + ">"));
        } else {
            lines.add(MM.parse("<" + Theme.C_MUTED + ">Everything is already up to date</"
                    + Theme.C_MUTED + ">"));
        }

        // Scripts section
        renderChatSection(lines, "Scripts", scriptResults);

        // Configs section
        renderChatSection(lines, "Configs", configResults);

        if (totalMigrated > 0) {
            lines.add(Component.empty());
            Component reloadHint = MM.parse("<" + Theme.C_MUTED + ">Run </" + Theme.C_MUTED + ">")
                    .append(MM.cmd("/cb reload"))
                    .append(MM.parse("<" + Theme.C_MUTED + "> to apply changes</"
                            + Theme.C_MUTED + ">"));
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

    private void renderChatSection(List<Component> lines, String title,
            List<FileResult> results) {
        int migrated = countStatus(results, FileStatus.MIGRATED);
        int skipped = countStatus(results, FileStatus.SKIPPED);
        int errors = countStatus(results, FileStatus.ERROR);

        lines.add(Component.empty());
        lines.add(MM.parse("<" + Theme.C_ACCENT + "><bold>" + title + "</bold></"
                + Theme.C_ACCENT + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Total:</" + Theme.C_MUTED + "> <"
                + Theme.C_ACCENT + ">" + results.size() + "</" + Theme.C_ACCENT + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Migrated:</" + Theme.C_MUTED + "> <"
                + Theme.C_SUCCESS + ">" + migrated + "</" + Theme.C_SUCCESS + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Skipped:</" + Theme.C_MUTED + "> <"
                + Theme.C_MUTED + ">" + skipped + "</" + Theme.C_MUTED + ">"));
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Errors:</" + Theme.C_MUTED + "> <"
                + Theme.C_ERROR + ">" + errors + "</" + Theme.C_ERROR + ">"));

        if (migrated > 0 || errors > 0) {
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
                        .parse("<" + Theme.C_ACCENT + ">" + Theme.SYMBOL_BULLET + "</"
                                + Theme.C_ACCENT + "> ")
                        .append(MM.parse("<white>" + r.filename + "</white>"))
                        .append(MM.parse(" <" + color + ">" + statusText + "</" + color + ">"));
                lines.add(header);

                if (!version.isEmpty()) {
                    lines.add(MM.parse("<" + Theme.C_MUTED + ">  " + version + "</"
                            + Theme.C_MUTED + ">"));
                }
                if (r.error != null && !r.error.isBlank()) {
                    lines.add(MM.parse("<" + Theme.C_ERROR + ">  " + r.error + "</"
                            + Theme.C_ERROR + ">"));
                }
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private long elapsedMs(long startNs) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
    }

    private static int countStatus(List<FileResult> results, FileStatus status) {
        int count = 0;
        for (FileResult r : results) {
            if (r.status == status) count++;
        }
        return count;
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
