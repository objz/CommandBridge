package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.DebugPrinter;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Report;
import dev.objz.commandbridge.velocity.ui.Report.Status;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ScriptCommand {

    public static final List<String> GROUPS = List.of(
            "permissions", "register", "defaults", "args", "commands");

    private static final String SECTION = "Scripts";
    private static final int PAGE_SIZE = 5;

    private final ScriptManager scriptManager;
    private final RegistrationManager registrationManager;

    public ScriptCommand(ScriptManager scriptManager, RegistrationManager registrationManager) {
        this.scriptManager = Objects.requireNonNull(scriptManager);
        this.registrationManager = Objects.requireNonNull(registrationManager);
    }

    public void list(CommandSource sender, int requestedPage) {
        List<Script> scripts = scriptManager.loaded();
        Report report = Report.of(SECTION);

        if (scripts.isEmpty()) {
            report.warn("No scripts loaded").send(sender);
            return;
        }

        int totalPages = (int) Math.ceil((double) scripts.size() / PAGE_SIZE);
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, scripts.size());

        report.section("Summary")
                .summary("Loaded", scriptManager.loaded().size(), Status.ACCENT)
                .summary("Enabled", scriptManager.enabled().size(), Status.SUCCESS)
                .summary("Disabled", scriptManager.disabled().size(), Status.WARN)
                .summary("Errors", (int) scriptManager.errors(), Status.ERROR);

        report.section("Sections");
        for (int i = start; i < end; i++) {
            Script s = scripts.get(i);
            String desc = s.description() != null ? s.description() : "no description";
            Status status = s.enabled() ? Status.SUCCESS : Status.ERROR;
            report.listItem(s.name(), desc, status, "/cb script show " + s.name());
        }

        if (totalPages > 1) {
            report.pager(page, totalPages, "/cb script list");
        }

        report.send(sender);
    }

    public void show(CommandSource sender, String name, String group) {
        var scriptOpt = scriptManager.findByName(name);
        if (scriptOpt.isEmpty()) {
            Report.of("Script").error("Unknown script '" + name + "'").send(sender);
            return;
        }
        Script s = scriptOpt.get();

        if (group == null || group.isBlank()) {
            renderOverview(sender, s);
            return;
        }

        String normalized = group.toLowerCase(Locale.ROOT);
        if (!GROUPS.contains(normalized)) {
            Report.of("Script")
                    .error("Unknown group '" + group + "'. Available: "
                            + String.join(", ", GROUPS))
                    .send(sender);
            return;
        }
        renderGroup(sender, s, normalized);
    }

    public void enable(CommandSource sender, String name) {
        applyToggle(sender, name, true);
    }

    public void disable(CommandSource sender, String name) {
        applyToggle(sender, name, false);
    }

    private void applyToggle(CommandSource sender, String name, boolean target) {
        ScriptManager.ToggleResult result = scriptManager.setEnabled(name, target);
        switch (result.status()) {
            case NOT_FOUND -> Report.of(SECTION)
                    .error("Unknown script '" + name + "'").send(sender);
            case IO_ERROR -> Report.of(SECTION)
                    .error("Failed to update script file: " + result.error()).send(sender);
            case UNCHANGED -> Report.of(SECTION)
                    .warn("Script '" + name + "' is already "
                            + (target ? "enabled" : "disabled"))
                    .send(sender);
            case CHANGED -> {
                registrationManager.load(scriptManager.enabled());
                registrationManager.reload();
                Report.of(SECTION)
                        .success("Script '" + name + "' "
                                + (target ? "enabled" : "disabled")
                                + " and pushed to clients")
                        .send(sender);
            }
        }
    }

    private void renderOverview(CommandSource sender, Script s) {
        if (new RenderContext(sender).isPlayer()) {
            renderChatOverview(sender, s);
        } else {
            renderConsoleOverview(s);
        }
    }

    private void renderChatOverview(CommandSource sender, Script s) {
        Status statusColor = s.enabled() ? Status.SUCCESS : Status.ERROR;
        String statusText = s.enabled() ? "Enabled" : "Disabled";
        String aliases = s.aliases() != null && !s.aliases().isEmpty()
                ? String.join(", ", s.aliases())
                : "none";
        int commandCount = s.commands() != null ? s.commands().size() : 0;
        int argCount = s.args() != null ? s.args().size() : 0;

        Report report = Report.of("Script")
                .title(s.name())
                .description(s.description());

        report.section("Summary")
                .kv("Status", statusText, statusColor)
                .kv("Version", String.valueOf(s.version()))
                .kv("Aliases", aliases)
                .kv("Register", formatTargets(s.register()))
                .kv("Args", String.valueOf(argCount))
                .kv("Commands", String.valueOf(commandCount));

        report.section("Sections");
        for (String groupName : GROUPS) {
            report.listItem(groupName, null, Status.NEUTRAL,
                    "/cb script show " + s.name() + " " + groupName);
        }

        report.send(sender);
    }

    private void renderConsoleOverview(Script s) {
        CliOutput output = CliOutput.create("Script").blankLine();
        output.appendRaw(DebugPrinter.printRecordOverview(s, s.name()));
        Log.info(output.build());
    }

    private void renderGroup(CommandSource sender, Script s, String group) {
        if (new RenderContext(sender).isPlayer()) {
            renderChatGroup(sender, s, group);
        } else {
            renderConsoleGroup(s, group);
        }
    }

    private void renderChatGroup(CommandSource sender, Script s, String group) {
        Report report = Report.of("Script");
        Object value = groupValue(s, group);
        if (value == null) {
            report.section(group).muted("(empty)").send(sender);
            return;
        }
        report.section(group);
        if (value instanceof List<?> list) {
            walkList(report, list, 1);
        } else {
            walkRecord(report, value, 1);
        }
        report.send(sender);
    }

    private void renderConsoleGroup(Script s, String group) {
        Object value = groupValue(s, group);
        CliOutput output = CliOutput.create("Script").blankLine();
        if (value instanceof List<?> list) {
            output.appendRaw(DebugPrinter.printList(list, group));
        } else if (value != null) {
            output.appendRaw(DebugPrinter.printRecord(value, group));
        } else {
            output.appendRaw(group).appendRaw(": <empty>\n");
        }
        Log.info(output.build());
    }

    private static Object groupValue(Script s, String group) {
        return switch (group) {
            case "permissions" -> s.permissions();
            case "register" -> s.register();
            case "defaults" -> s.defaults();
            case "args" -> s.args();
            case "commands" -> s.commands();
            default -> null;
        };
    }

    private static void walkRecord(Report report, Object record, int depth) {
        if (record == null || !record.getClass().isRecord()) {
            return;
        }
        String indent = "  ".repeat(Math.max(0, depth));
        for (var rc : record.getClass().getRecordComponents()) {
            String key = rc.getName().replaceAll("(?<!^)([A-Z])", "-$1")
                    .toLowerCase(Locale.ROOT);
            Object value;
            try {
                value = rc.getAccessor().invoke(record);
            } catch (ReflectiveOperationException e) {
                value = "<error>";
            }
            if (value != null && value.getClass().isRecord()) {
                report.section(indent + key);
                walkRecord(report, value, depth + 1);
            } else if (value instanceof List<?> list) {
                report.section(indent + key);
                walkList(report, list, depth + 1);
            } else {
                report.kv(indent + key, formatValue(value));
            }
        }
    }

    private static void walkList(Report report, List<?> items, int depth) {
        if (items == null || items.isEmpty()) {
            String indent = "  ".repeat(Math.max(0, depth));
            report.muted(indent + "(empty)");
            return;
        }
        String indent = "  ".repeat(Math.max(0, depth));
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item != null && item.getClass().isRecord()) {
                report.muted(indent + "- item " + (i + 1));
                walkRecord(report, item, depth + 1);
            } else {
                report.kv(indent + "-", formatValue(item));
            }
        }
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "<none>";
        }
        if (value instanceof String s && s.isEmpty()) {
            return "\"\"";
        }
        return String.valueOf(value);
    }

    private static String formatTargets(List<IdMapping> targets) {
        if (targets == null || targets.isEmpty()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < targets.size(); i++) {
            IdMapping t = targets.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(t.id() != null ? t.id() : "?");
            if (t.location() != null) {
                sb.append(" (").append(t.location().name()).append(")");
            }
        }
        return sb.toString();
    }
}
