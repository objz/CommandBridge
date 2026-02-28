package dev.objz.commandbridge.scripting;

import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.Defaults;
import dev.objz.commandbridge.scripting.model.records.Server;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DebugPrinter {

    private static final class SyntaxTheme {
        private final boolean enabled;

        private final String reset;
        private final String section;
        private final String property;
        private final String string;
        private final String number;
        private final String booleanTrue;
        private final String booleanFalse;
        private final String keyword;
        private final String comment;
        private final String variable;
        private final String error;
        private final String punctuation;
        private final String operator;
        private final String yellow;

        private SyntaxTheme(boolean enabled) {
            this.enabled = enabled;

            if (enabled) {
                reset = "\u001B[0m";
                section = "\u001B[38;2;78;201;176m";
                property = "\u001B[38;2;156;220;254m";
                string = "\u001B[38;2;206;145;120m";
                number = "\u001B[38;2;181;206;168m";
                booleanTrue = "\u001B[38;2;86;156;214m";
                booleanFalse = "\u001B[38;2;86;156;214m";
                keyword = "\u001B[38;2;197;134;192m";
                comment = "\u001B[38;2;106;153;85m";
                variable = "\u001B[38;2;220;220;170m";
                error = "\u001B[38;2;244;71;71m";
                punctuation = "\u001B[38;2;212;212;212m";
                operator = "\u001B[38;2;212;212;212m";
                yellow = "\u001B[33m";
            } else {
                reset = section = property = string = number = "";
                booleanTrue = booleanFalse = keyword = comment = "";
                variable = error = punctuation = operator = yellow = "";
            }
        }

        static SyntaxTheme create() {
            boolean ansiEnabled = !"0".equals(System.getenv().getOrDefault("CB_DEBUG_ANSI", "1"))
                    && !"false".equalsIgnoreCase(System.getProperty("cb.debug.ansi", "true"));
            return new SyntaxTheme(ansiEnabled);
        }
    }

    private static final char BOX_TL = '╭', BOX_TR = '╮', BOX_BL = '╰', BOX_BR = '╯';
    private static final char BOX_H = '─', BOX_V = '│';
    private static final int PANEL_GAP = 2;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]*)}");
    private static final String NONE_VALUE = "<none>";
    private static final String INDENT_UNIT = "  ";
    private static final String LIST_MARKER = "- ";
    private static final String OVERRIDE_MARKER = "*";
    private static final int MIN_PANEL_WIDTH = 32;
    private static final int PANELS_PER_ROW = 2;

    private DebugPrinter() {
    }

    public static String print(Script script) {
        SyntaxTheme theme = SyntaxTheme.create();
        List<String> contentLines = buildScriptContent(script, theme);
        String title = formatTitle(script, theme);
        int panelWidth = calculatePanelWidth(contentLines);
        return renderPanel(title, contentLines, panelWidth, theme);
    }

    public static String printGrid(List<Script> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return "";
        }

        SyntaxTheme theme = SyntaxTheme.create();

        List<Panel> panels = new ArrayList<>(scripts.size());
        for (Script script : scripts) {
            List<String> content = buildScriptContent(script, theme);
            String title = formatTitle(script, theme);
            int width = calculatePanelWidth(content);
            List<String> box = createBox(title, content, width, theme);
            panels.add(new Panel(box, getVisibleWidth(box.get(0))));
        }

        return layoutPanelsInGrid(panels);
    }

    private static List<String> buildScriptContent(Script script, SyntaxTheme theme) {
        ContentBuilder builder = new ContentBuilder(theme);
        Set<String> declaredArgs = extractDeclaredArgNames(script);

        builder.addPropertyNoIndent("version", script.version());
        builder.addPropertyNoIndent("name", script.name());
        builder.addPropertyNoIndent("enabled", script.enabled());
        builder.addPropertyNoIndent("description", script.description());
        builder.addPropertyList("aliases", script.aliases());
        builder.addBlankLine();

        builder.addSection("permissions");
        builder.increaseIndent();
        builder.addProperty("enabled", script.permissions().enabled());
        builder.addProperty("silent", script.permissions().silent());
        builder.decreaseIndent();
        builder.addBlankLine();

        builder.addSection("register");
        addRegisterContent(builder, script.register());
        builder.addBlankLine();

        builder.addSection("defaults");
        addDefaultsContent(builder, script.defaults());
        builder.addBlankLine();

        builder.addSection("args");
        addArgsContent(builder, script.args());
        builder.addBlankLine();

        builder.addSection("commands");
        addCommandsContent(builder, script.commands(), script.defaults(), declaredArgs);

        return builder.getLines();
    }

    private static void addRegisterContent(ContentBuilder builder, List<IdMapping> register) {
        if (register == null || register.isEmpty()) {
            builder.increaseIndent();
            builder.addNoneValue();
            builder.decreaseIndent();
            return;
        }

        builder.increaseIndent();
        for (IdMapping mapping : register) {
            builder.startListItem();
            builder.addProperty("id", mapping.id());
            builder.addProperty("location", mapping.location());
        }
        builder.decreaseIndent();
    }

    private static void addDefaultsContent(ContentBuilder builder, Defaults defaults) {
        if (defaults == null) {
            builder.increaseIndent();
            builder.addNoneValue();
            builder.decreaseIndent();
            return;
        }

        builder.increaseIndent();
        builder.addProperty("run-as", defaults.runAs());

        if (defaults.execute() == null || defaults.execute().isEmpty()) {
            builder.addPropertyWithValue("execute", NONE_VALUE);
        } else {
            builder.addPropertyLabel("execute");
            builder.increaseIndent();
            for (IdMapping id : defaults.execute()) {
                builder.startListItem();
                builder.addProperty("id", id.id());
                builder.addProperty("location", id.location());
            }
            builder.decreaseIndent();
        }

        builder.addPropertyLabel("server");
        Server server = defaults.server();
        if (server == null) {
            builder.increaseIndent();
            builder.addNoneValue();
            builder.decreaseIndent();
        } else {
            builder.increaseIndent();
            builder.addProperty("target-required", server.targetRequired());
            builder.addProperty("schedule-online", server.scheduleOnline());
            builder.decreaseIndent();
        }

        builder.addProperty("delay", defaults.delay());
        builder.addProperty("cooldown", defaults.cooldown());
        builder.decreaseIndent();
    }

    private static void addArgsContent(ContentBuilder builder, List<ArgMapping> args) {
        if (args == null || args.isEmpty()) {
            builder.increaseIndent();
            builder.addNoneValue();
            builder.decreaseIndent();
            return;
        }

        builder.increaseIndent();
        for (ArgMapping arg : args) {
            builder.startListItem();
            builder.addProperty("name", arg.name());
            builder.addProperty("required", arg.required());
            builder.addProperty("type", arg.type());
        }
        builder.decreaseIndent();
    }

    private static void addCommandsContent(ContentBuilder builder, List<CmdMapping> commands,
            Defaults defaults, Set<String> declaredArgs) {
        if (commands == null || commands.isEmpty()) {
            builder.increaseIndent();
            builder.addNoneValue();
            builder.decreaseIndent();
            return;
        }

        builder.increaseIndent();
        for (CmdMapping cmd : commands) {
            builder.startListItem();
            builder.addPropertyWithHighlightedCommand("command", cmd.command(), declaredArgs);

            boolean overridesRunAs = isOverride(cmd.runAs(), defaults, d -> d.runAs());
            boolean overridesExecute = isOverride(cmd.execute(), defaults, d -> d.execute());
            boolean overridesServer = isServerOverride(cmd.server(), defaults);
            boolean overridesDelay = isOverride(cmd.delay(), defaults, d -> d.delay());
            boolean overridesCooldown = isOverride(cmd.cooldown(), defaults, d -> d.cooldown());

            builder.addProperty("run-as", cmd.runAs(), overridesRunAs);

            if (cmd.execute() == null) {
                builder.addPropertyWithValue("execute", NONE_VALUE, overridesExecute);
            } else {
                builder.addPropertyLabel("execute", overridesExecute);
                builder.increaseIndent();
                for (IdMapping id : cmd.execute()) {
                    builder.startListItem();
                    builder.addProperty("id", id.id());
                    builder.addProperty("location", id.location());
                }
                builder.decreaseIndent();
            }

            builder.addPropertyLabel("server", overridesServer);
            Server server = cmd.server();
            if (server == null) {
                builder.increaseIndent();
                builder.addNoneValue();
                builder.decreaseIndent();
            } else {
                builder.increaseIndent();
                builder.addProperty("target-required", server.targetRequired());
                builder.addProperty("schedule-online", server.scheduleOnline());
                builder.decreaseIndent();
            }

            builder.addProperty("delay", cmd.delay(), overridesDelay);
            builder.addProperty("cooldown", cmd.cooldown(), overridesCooldown);
        }
        builder.decreaseIndent();
    }

    private static class ContentBuilder {
        private final SyntaxTheme theme;
        private final List<String> lines;
        private int indentLevel;
        private boolean needsListItemStart;

        ContentBuilder(SyntaxTheme theme) {
            this.theme = theme;
            this.lines = new ArrayList<>();
            this.indentLevel = 0;
            this.needsListItemStart = false;
        }

        void increaseIndent() {
            indentLevel++;
        }

        void decreaseIndent() {
            if (indentLevel > 0)
                indentLevel--;
            needsListItemStart = false;
        }

        void startListItem() {
            needsListItemStart = true;
        }

        void addBlankLine() {
            lines.add("");
            needsListItemStart = false;
        }

        void addSection(String name) {
            lines.add(getIndentString() + theme.section + name + theme.reset);
            needsListItemStart = false;
        }

        void addPropertyNoIndent(String key, Object value) {
            String formattedKey = theme.property + key + theme.reset;
            String formattedValue = formatValue(value);
            lines.add(formattedKey + theme.punctuation + ": " + theme.reset + formattedValue);
            needsListItemStart = false;
        }

        void addProperty(String key, Object value) {
            addProperty(key, value, false);
        }

        void addProperty(String key, Object value, boolean override) {
            String formattedKey = theme.property + key + theme.reset;
            String formattedValue = formatValue(value);
            String marker = override ? theme.yellow + OVERRIDE_MARKER + theme.reset : "";

            String indent = getPropertyIndent();
            lines.add(indent + formattedKey + theme.punctuation + ": " + theme.reset + formattedValue + marker);
            needsListItemStart = false;
        }

        void addPropertyWithValue(String key, String value) {
            addPropertyWithValue(key, value, false);
        }

        void addPropertyWithValue(String key, String value, boolean override) {
            String formattedKey = theme.property + key + theme.reset;
            String formattedValue = theme.string + value + theme.reset;
            String marker = override ? theme.yellow + OVERRIDE_MARKER + theme.reset : "";

            String indent = getPropertyIndent();
            lines.add(indent + formattedKey + theme.punctuation + ": " + theme.reset + formattedValue + marker);
            needsListItemStart = false;
        }

        void addPropertyLabel(String key) {
            addPropertyLabel(key, false);
        }

        void addPropertyLabel(String key, boolean override) {
            String formattedKey = theme.property + key + theme.reset;
            String marker = override ? theme.yellow + OVERRIDE_MARKER + theme.reset : "";

            String indent = getPropertyIndent();
            lines.add(indent + formattedKey + theme.punctuation + ":" + theme.reset + marker);
            needsListItemStart = false;
        }

        void addPropertyList(String key, List<?> values) {
            String formattedKey = theme.property + key + theme.reset;
            if (values == null || values.isEmpty()) {
                lines.add(formattedKey + theme.punctuation + ": " + theme.reset +
                        theme.string + NONE_VALUE + theme.reset);
            } else {
                String formattedList = formatList(values);
                lines.add(formattedKey + theme.punctuation + ": " + theme.reset + formattedList);
            }
            needsListItemStart = false;
        }

        void addPropertyWithHighlightedCommand(String key, String command, Set<String> validArgs) {
            String formattedKey = theme.property + key + theme.reset;
            String highlighted = highlightPlaceholders(command, validArgs);

            String indent = getPropertyIndent();
            lines.add(indent + formattedKey + theme.punctuation + ": " + theme.reset + highlighted);
            needsListItemStart = false;
        }

        void addNoneValue() {
            lines.add(getIndentString() + theme.string + NONE_VALUE + theme.reset);
            needsListItemStart = false;
        }

        List<String> getLines() {
            return lines;
        }

        private String getIndentString() {
            return INDENT_UNIT.repeat(Math.max(0, indentLevel));
        }

        private String getPropertyIndent() {
            if (needsListItemStart) {
                String baseIndent = INDENT_UNIT.repeat(Math.max(0, indentLevel));
                String marker = theme.punctuation + LIST_MARKER + theme.reset;
                return baseIndent + marker;
            }
            return INDENT_UNIT.repeat(Math.max(0, indentLevel + 1));
        }

        private String formatList(List<?> values) {
            if (values == null || values.isEmpty()) {
                return theme.punctuation + "[]" + theme.reset;
            }

            StringBuilder result = new StringBuilder();
            result.append(theme.punctuation).append("[").append(theme.reset);

            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                result.append(theme.string).append(value).append(theme.reset);

                if (i < values.size() - 1) {
                    result.append(theme.punctuation).append(", ").append(theme.reset);
                }
            }

            result.append(theme.punctuation).append("]").append(theme.reset);
            return result.toString();
        }

        private String formatValue(Object value) {
            if (value == null) {
                return theme.string + NONE_VALUE + theme.reset;
            }
            if (value instanceof Boolean bool) {
                String color = bool ? theme.booleanTrue : theme.booleanFalse;
                return color + value + theme.reset;
            }
            if (value instanceof Number) {
                return theme.number + value + theme.reset;
            }
            if (value instanceof java.time.Duration duration) {
                return theme.comment + formatDuration(duration) + theme.reset;
            }
            if (value instanceof Enum<?>) {
                return theme.keyword + value + theme.reset;
            }
            return theme.string + value + theme.reset;
        }

        private String highlightPlaceholders(String text, Set<String> validArgs) {
            if (text == null || text.isBlank()) {
                return theme.string + NONE_VALUE + theme.reset;
            }

            StringBuilder result = new StringBuilder();
            int lastIndex = 0;
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);

            while (matcher.find()) {
                if (matcher.start() > lastIndex) {
                    result.append(theme.string)
                            .append(text.substring(lastIndex, matcher.start()))
                            .append(theme.reset);
                }

                String argName = matcher.group(1);
                boolean isEmpty = argName == null || argName.isBlank();
                boolean isValid = !isEmpty && validArgs.contains(argName);

                if (isEmpty || !isValid) {
                    result.append(theme.error)
                            .append("${").append(argName == null ? "" : argName).append("}")
                            .append(theme.reset);
                } else {
                    result.append(theme.variable)
                            .append("${").append(argName).append("}")
                            .append(theme.reset);
                }

                lastIndex = matcher.end();
            }

            if (lastIndex < text.length()) {
                result.append(theme.string)
                        .append(text.substring(lastIndex))
                        .append(theme.reset);
            }

            return result.toString();
        }
    }

    private static class Panel {
        final List<String> lines;
        final int width;

        Panel(List<String> lines, int width) {
            this.lines = lines;
            this.width = width;
        }
    }

    private static String formatTitle(Script script, SyntaxTheme theme) {
        String name = script.name() != null ? script.name() : "Script";
        return theme.section + name + theme.reset;
    }

    private static String formatDuration(java.time.Duration duration) {
        if (duration == null) {
            return NONE_VALUE;
        }
        long millis = duration.toMillis();
        return (millis % 1000 == 0) ? (millis / 1000) + "s" : millis + "ms";
    }

    private static List<String> createBox(String title, List<String> content, int width, SyntaxTheme theme) {
        List<String> box = new ArrayList<>(content.size() + 2);
        int innerWidth = width - 2;

        String titlePadded = " " + title + " ";
        int titleVisibleWidth = getVisibleWidth(title) + 2;
        int leftPadding = Math.max(1, (innerWidth - titleVisibleWidth) / 2);
        int rightPadding = innerWidth - titleVisibleWidth - leftPadding;

        box.add(BOX_TL + repeat(BOX_H, leftPadding) + titlePadded +
                repeat(BOX_H, Math.max(0, rightPadding)) + BOX_TR);

        for (String line : content) {
            box.add(BOX_V + padToWidth(line, innerWidth) + BOX_V);
        }

        box.add(BOX_BL + repeat(BOX_H, innerWidth) + BOX_BR);

        return box;
    }

    private static String renderPanel(String title, List<String> content, int width, SyntaxTheme theme) {
        StringBuilder output = new StringBuilder();
        for (String line : createBox(title, content, width, theme)) {
            output.append(line).append('\n');
        }
        return output.toString();
    }

    private static String layoutPanelsInGrid(List<Panel> panels) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < panels.size(); i += PANELS_PER_ROW) {
            List<Panel> row = new ArrayList<>();
            for (int j = 0; j < PANELS_PER_ROW && (i + j) < panels.size(); j++) {
                row.add(panels.get(i + j));
            }
            renderRow(output, row);
        }

        return output.toString();
    }

    private static void renderRow(StringBuilder output, List<Panel> panels) {
        int maxHeight = panels.stream()
                .mapToInt(p -> p.lines.size())
                .max()
                .orElse(0);

        for (int row = 0; row < maxHeight; row++) {
            for (int col = 0; col < panels.size(); col++) {
                Panel panel = panels.get(col);

                if (row < panel.lines.size()) {
                    output.append(panel.lines.get(row));
                } else {
                    output.append(" ".repeat(panel.width));
                }

                if (col + 1 < panels.size()) {
                    output.append(" ".repeat(PANEL_GAP));
                }
            }
            output.append('\n');
        }
    }

    private static Set<String> extractDeclaredArgNames(Script script) {
        Set<String> argNames = new LinkedHashSet<>();
        if (script.args() != null) {
            for (ArgMapping arg : script.args()) {
                if (arg != null && arg.name() != null && !arg.name().isBlank()) {
                    argNames.add(arg.name());
                }
            }
        }
        return argNames;
    }

    private static <T, R> boolean isOverride(R value, T defaults, java.util.function.Function<T, R> getter) {
        return value != null && defaults != null && !Objects.equals(value, getter.apply(defaults));
    }

    private static boolean isServerOverride(Server server, Defaults defaults) {
        if (server == null || defaults == null || defaults.server() == null) {
            return server != null;
        }
        Server defaultServer = defaults.server();
        return !Objects.equals(server.targetRequired(), defaultServer.targetRequired())
                || !Objects.equals(server.scheduleOnline(), defaultServer.scheduleOnline());
    }

    private static int getVisibleWidth(String text) {
        return stripAnsiCodes(text).length();
    }

    private static String stripAnsiCodes(String text) {
        return text.replaceAll("\\u001B\\[[;?\\d]*[ -/]*[@-~]", "");
    }

    private static String padToWidth(String text, int width) {
        int visibleWidth = getVisibleWidth(text);
        if (visibleWidth == width) {
            return text;
        }
        if (visibleWidth < width) {
            return text + " ".repeat(width - visibleWidth);
        }

        StringBuilder result = new StringBuilder();
        int visible = 0;
        boolean inEscape = false;

        for (int i = 0; i < text.length() && visible < width; i++) {
            char ch = text.charAt(i);
            if (ch == '\u001B') {
                inEscape = true;
            }
            if (!inEscape) {
                visible++;
            }
            result.append(ch);
            if (inEscape && ch == 'm') {
                inEscape = false;
            }
        }

        if (visible < width) {
            result.append(" ".repeat(width - visible));
        }

        return result.toString();
    }

    private static int calculatePanelWidth(List<String> content) {
        int contentWidth = content.stream()
                .mapToInt(DebugPrinter::getVisibleWidth)
                .max()
                .orElse(0);
        return Math.max(MIN_PANEL_WIDTH, contentWidth + 4);
    }

    private static String repeat(char character, int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = new char[count];
        Arrays.fill(chars, character);
        return new String(chars);
    }
}
