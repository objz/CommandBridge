package dev.objz.commandbridge.velocity.ui.cli;

import dev.objz.commandbridge.velocity.ui.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CliTable {
    public enum Align {
        LEFT,
        RIGHT
    }

    public static final class Column {
        final String header;
        final Align align;
        final int weight;
        final int minWidth;

        Column(String header, Align align, int weight, int minWidth) {
            this.header = header;
            this.align = align;
            this.weight = Math.max(1, weight);
            this.minWidth = Math.max(1, minWidth);
        }
    }

    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");
    private static final int MIN_COL_WIDTH = 3;

    private final List<Column> columns = new ArrayList<>();
    private final List<String[]> rows = new ArrayList<>();
    private int width = CliLayout.DEFAULT_WIDTH;
    private int gap = 2;

    public CliTable width(int width) {
        this.width = Math.max(20, width);
        return this;
    }

    public CliTable gap(int gap) {
        this.gap = Math.max(1, gap);
        return this;
    }

    public CliTable addColumn(String header) {
        return addColumn(header, Align.LEFT, 1, header.length());
    }

    public CliTable addColumn(String header, Align align, int weight, int minWidth) {
        columns.add(new Column(header, align, weight, minWidth));
        return this;
    }

    public void addRow(String... values) {
        if (values.length != columns.size()) {
            throw new IllegalArgumentException("Row length mismatch");
        }
        rows.add(values);
    }

    public String render() {
        if (columns.isEmpty()) {
            return "";
        }

        int[] widths = computeWidths();
        StringBuilder sb = new StringBuilder();

        sb.append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
        sb.append(renderLine(headers(), widths));
        sb.append(Theme.ANSI_RESET).append("\n");
        sb.append(Theme.ANSI_SEP).append("-".repeat(width)).append(Theme.ANSI_RESET).append("\n");

        for (String[] row : rows) {
            sb.append(renderLine(row, widths)).append("\n");
        }

        return sb.toString();
    }

    private String[] headers() {
        String[] headers = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            headers[i] = columns.get(i).header;
        }
        return headers;
    }

    private int[] computeWidths() {
        int count = columns.size();
        int[] widths = new int[count];
        int[] weights = new int[count];

        int available = Math.max(1, width - gap * (count - 1));

        int sum = 0;
        for (int i = 0; i < count; i++) {
            Column col = columns.get(i);
            int headerLen = visibleLength(col.header);
            int min = Math.max(col.minWidth, headerLen);
            widths[i] = Math.max(MIN_COL_WIDTH, min);
            weights[i] = col.weight;
            sum += widths[i];
        }

        if (sum < available) {
            int leftover = available - sum;
            int totalWeight = 0;
            for (int w : weights)
                totalWeight += w;
            int used = 0;
            for (int i = 0; i < count; i++) {
                int add = (int) Math.floor((double) leftover * weights[i] / totalWeight);
                widths[i] += add;
                used += add;
            }
            int remaining = leftover - used;
            int idx = 0;
            while (remaining > 0) {
                widths[idx % count] += 1;
                remaining--;
                idx++;
            }
        } else if (sum > available) {
            int overflow = sum - available;
            while (overflow > 0) {
                int idx = widestColumn(widths);
                if (widths[idx] <= MIN_COL_WIDTH) {
                    break;
                }
                widths[idx] -= 1;
                overflow--;
            }
        }

        return widths;
    }

    private int widestColumn(int[] widths) {
        int idx = 0;
        for (int i = 1; i < widths.length; i++) {
            if (widths[i] > widths[idx]) {
                idx = i;
            }
        }
        return idx;
    }

    private String renderLine(String[] values, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(" ".repeat(gap));
            }
            Column col = columns.get(i);
            line.append(pad(values[i], widths[i], col.align));
        }
        return line.toString();
    }

    private String pad(String value, int width, Align align) {
        String trimmed = truncate(value, width);
        int visible = visibleLength(trimmed);
        int pad = Math.max(0, width - visible);
        if (align == Align.RIGHT) {
            return " ".repeat(pad) + trimmed;
        }
        return trimmed + " ".repeat(pad);
    }

    private String truncate(String value, int width) {
        String clean = stripAnsi(value);
        if (clean.length() <= width) {
            return value;
        }
        String truncated;
        if (width <= 3) {
            truncated = clean.substring(0, width);
        } else {
            truncated = clean.substring(0, width - 3) + "...";
        }
        String prefix = leadingAnsi(value);
        String suffix = value.contains("\u001B[") ? Theme.ANSI_RESET : "";
        return prefix + truncated + suffix;
    }

    private String leadingAnsi(String value) {
        Matcher matcher = ANSI_PATTERN.matcher(value);
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (matcher.find(index) && matcher.start() == index) {
            sb.append(matcher.group());
            index = matcher.end();
        }
        return sb.toString();
    }

    private int visibleLength(String value) {
        return stripAnsi(value).length();
    }

    private String stripAnsi(String value) {
        return ANSI_PATTERN.matcher(value).replaceAll("");
    }
}
