package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.config.ConfigKeys;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.DebugPrinter;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Report;
import dev.objz.commandbridge.velocity.ui.Report.Status;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ConfigCommand {

    private static final String SECTION = "Config";

    private final ConfigManager<VelocityConfig> configManager;

    public ConfigCommand(ConfigManager<VelocityConfig> configManager) {
        this.configManager = Objects.requireNonNull(configManager);
    }

    public void show(CommandSource sender, String key) {
        VelocityConfig cfg = configManager.current();
        if (cfg == null) {
            Report.of(SECTION).error("Config not loaded").send(sender);
            return;
        }

        if (key == null || key.isBlank()) {
            renderOverview(sender, cfg);
            return;
        }

        Optional<RecordComponent> compOpt = lookupTopLevelComponent(key);
        if (compOpt.isEmpty()) {
            Report.of(SECTION).error("Unknown config key '" + key + "'").send(sender);
            return;
        }
        RecordComponent rc = compOpt.get();
        String resolvedKey = ConfigKeys.yamlKeyFor(VelocityConfig.class, rc);
        Object value = accessorValue(cfg, rc);
        renderSection(sender, resolvedKey, value);
    }

    public void reload(CommandSource sender) {
        boolean ok = configManager.reload();
        VelocityConfig cfg = configManager.current();
        if (!ok || cfg == null) {
            Report.of(SECTION)
                    .error("Failed to reload config — check console for details")
                    .send(sender);
            return;
        }
        Log.setDebug(cfg.debug());
        Report.of(SECTION).success("Config reloaded").send(sender);
    }

    // TODO: implement /cb config set <key> <value>
    // Needs a path-walker that descends the record tree, parses the value into the right type
    // (boolean, int, enum, string, Duration), validates via VelocityConfigProfile.normalize,
    // and writes back to config.yml without trashing comments. Out of scope for this commit.

    private void renderOverview(CommandSource sender, VelocityConfig cfg) {
        if (new RenderContext(sender).isPlayer()) {
            renderChatOverview(sender, cfg);
        } else {
            renderConsoleOverview(cfg);
        }
    }

    private void renderChatOverview(CommandSource sender, VelocityConfig cfg) {
        Report report = Report.of(SECTION);
        boolean wroteSummary = false;
        for (RecordComponent rc : VelocityConfig.class.getRecordComponents()) {
            Object value = accessorValue(cfg, rc);
            if (isNested(value)) {
                continue;
            }
            if (!wroteSummary) {
                report.section("Summary");
                wroteSummary = true;
            }
            String key = ConfigKeys.yamlKeyFor(VelocityConfig.class, rc);
            report.kv(key, formatValue(value), inferStatus(value));
        }

        boolean wroteSections = false;
        for (RecordComponent rc : VelocityConfig.class.getRecordComponents()) {
            Object value = accessorValue(cfg, rc);
            if (!isNested(value)) {
                continue;
            }
            if (!wroteSections) {
                report.section("Sections");
                wroteSections = true;
            }
            String key = ConfigKeys.yamlKeyFor(VelocityConfig.class, rc);
            report.listItem(key, null, Status.NEUTRAL, "/cb config show " + key);
        }

        report.send(sender);
    }

    private void renderConsoleOverview(VelocityConfig cfg) {
        CliOutput output = CliOutput.create(SECTION).blankLine();
        output.appendRaw(DebugPrinter.printRecordOverview(cfg, "velocity"));
        Log.info(output.build());
    }

    private void renderSection(CommandSource sender, String key, Object value) {
        if (new RenderContext(sender).isPlayer()) {
            renderChatSection(sender, key, value);
        } else {
            renderConsoleSection(key, value);
        }
    }

    private void renderChatSection(CommandSource sender, String key, Object value) {
        Report report = Report.of(SECTION);
        if (value != null && value.getClass().isRecord()) {
            report.section(key);
            walk(report, value, 1);
        } else {
            report.kv(key, formatValue(value));
        }
        report.send(sender);
    }

    private void renderConsoleSection(String key, Object value) {
        CliOutput output = CliOutput.create(SECTION).blankLine();
        if (value != null && value.getClass().isRecord()) {
            output.appendRaw(DebugPrinter.printRecord(value, key));
        } else {
            output.appendRaw(key).appendRaw(": ").appendRaw(formatValue(value)).appendRaw("\n");
        }
        Log.info(output.build());
    }

    private Optional<RecordComponent> lookupTopLevelComponent(String key) {
        for (RecordComponent rc : VelocityConfig.class.getRecordComponents()) {
            String yaml = ConfigKeys.yamlKeyFor(VelocityConfig.class, rc);
            if (yaml.equalsIgnoreCase(key)) {
                return Optional.of(rc);
            }
        }
        return Optional.empty();
    }

    private static void walk(Report report, Object record, int depth) {
        if (record == null || !record.getClass().isRecord()) {
            return;
        }
        for (RecordComponent rc : record.getClass().getRecordComponents()) {
            String key = ConfigKeys.yamlKeyFor(record.getClass(), rc);
            Object value = accessorValue(record, rc);
            String indented = "  ".repeat(Math.max(0, depth)) + key;
            if (value != null && value.getClass().isRecord()) {
                report.section(indented);
                walk(report, value, depth + 1);
            } else {
                report.kv(indented, formatValue(value));
            }
        }
    }

    private static boolean isNested(Object value) {
        if (value == null) {
            return false;
        }
        if (value.getClass().isRecord()) {
            return true;
        }
        return value instanceof List<?> list && !list.isEmpty()
                && list.get(0) != null && list.get(0).getClass().isRecord();
    }

    private static Status inferStatus(Object value) {
        if (value instanceof Boolean b) {
            return b ? Status.SUCCESS : Status.NEUTRAL;
        }
        if (value instanceof Number) {
            return Status.ACCENT;
        }
        if (value instanceof Enum<?>) {
            return Status.ACCENT;
        }
        return Status.NEUTRAL;
    }

    private static Object accessorValue(Object record, RecordComponent rc) {
        try {
            return rc.getAccessor().invoke(record);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return "<error>";
        }
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s && s.isEmpty()) {
            return "\"\"";
        }
        if (value instanceof List<?> list) {
            List<String> parts = new ArrayList<>(list.size());
            for (Object o : list) {
                parts.add(String.valueOf(o));
            }
            return "[" + String.join(", ", parts) + "]";
        }
        return String.valueOf(value);
    }
}
