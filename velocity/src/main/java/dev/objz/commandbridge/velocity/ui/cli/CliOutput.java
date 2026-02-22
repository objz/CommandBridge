package dev.objz.commandbridge.velocity.ui.cli;

import dev.objz.commandbridge.velocity.ui.Theme;

public final class CliOutput {
    private final StringBuilder sb;
    private final int width;

    private CliOutput(String title) {
        this.sb = new StringBuilder();
        this.width = CliLayout.DEFAULT_WIDTH;
        CliLayout.appendHeader(sb, title, width);
    }

    public static CliOutput create(String title) {
        return new CliOutput(title);
    }

    public CliOutput blankLine() {
        sb.append("\n");
        return this;
    }

    public CliOutput line(String text) {
        sb.append(text).append("\n");
        return this;
    }

    public CliOutput muted(String text) {
        sb.append(Theme.ANSI_MUTED).append(text).append(Theme.ANSI_RESET).append("\n");
        return this;
    }

    public CliOutput accent(String text) {
        sb.append(Theme.ANSI_ACCENT).append(text).append(Theme.ANSI_RESET).append("\n");
        return this;
    }

    public CliOutput success(String text) {
        sb.append(Theme.ANSI_SUCCESS).append(text).append(Theme.ANSI_RESET).append("\n");
        return this;
    }

    public CliOutput warn(String text) {
        sb.append(Theme.ANSI_WARN).append(text).append(Theme.ANSI_RESET).append("\n");
        return this;
    }

    public CliOutput error(String text) {
        sb.append(Theme.ANSI_ERROR).append(text).append(Theme.ANSI_RESET).append("\n");
        return this;
    }

    public CliOutput appendRaw(String raw) {
        sb.append(raw);
        return this;
    }

    public CliOutput appendRawLine(String raw) {
        sb.append(raw).append("\n");
        return this;
    }

    public int width() {
        return width;
    }

    public String build() {
        return sb.toString();
    }
}
