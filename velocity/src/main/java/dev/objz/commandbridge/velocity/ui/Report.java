package dev.objz.commandbridge.velocity.ui;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Single source of truth for command output. Each builder method appends to
 * both a chat (MiniMessage Component) track and a console (ANSI) track in
 * lockstep, so a command only describes the content once and the right path
 * is chosen at send time.
 */
public final class Report {

    public enum Status {
        NEUTRAL, SUCCESS, WARN, ERROR, ACCENT
    }

    public record Action(String label, String command, String hover) {
        public static Action of(String label, String command, String hover) {
            return new Action(label, command, hover);
        }
    }

    private final String section;
    private final List<Component> chat = new ArrayList<>();
    private final CliOutput console;

    private Report(String section) {
        this.section = Objects.requireNonNull(section);
        this.console = CliOutput.create(section).blankLine();
    }

    public static Report of(String section) {
        return new Report(section);
    }

    /* ---------- headings ---------- */

    public Report title(String text) {
        chat.add(MM.title(text));
        console.appendRaw(Theme.ANSI_BOLD).appendRaw(Theme.ANSI_ACCENT)
                .appendRaw(safe(text)).appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        return this;
    }

    public Report description(String text) {
        if (text == null || text.isBlank()) {
            return this;
        }
        chat.add(MM.muted(text));
        console.muted(text);
        return this;
    }

    public Report section(String name) {
        chat.add(Component.empty());
        chat.add(MM.accent(name));
        console.blankLine();
        console.accent(name);
        return this;
    }

    /* ---------- content ---------- */

    public Report kv(String key, String value) {
        return kv(key, value, Status.NEUTRAL);
    }

    public Report kv(String key, String value, Status status) {
        String chatColor = chatColor(status);
        chat.add(MM.parse("<" + Theme.C_MUTED + ">" + safe(key) + ":</" + Theme.C_MUTED + "> "
                + "<" + chatColor + ">" + escape(value) + "</" + chatColor + ">"));

        String ansiColor = ansiColor(status);
        console.appendRaw(Theme.ANSI_MUTED).appendRaw(safe(key)).appendRaw(":")
                .appendRaw(Theme.ANSI_RESET).appendRaw(" ")
                .appendRaw(ansiColor).appendRaw(safe(value))
                .appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        return this;
    }

    public Report bullet(String text) {
        chat.add(MM.parse("<" + Theme.C_ACCENT + ">" + Theme.SYMBOL_BULLET + "</"
                + Theme.C_ACCENT + "> <white>" + escape(text) + "</white>"));
        console.appendRaw(Theme.ANSI_ACCENT).appendRaw(Theme.SYMBOL_BULLET).appendRaw(" ")
                .appendRaw(Theme.ANSI_RESET).appendRaw(safe(text)).appendRaw("\n");
        return this;
    }

    /** Bullet with a pre-built chat component (e.g. clickable command). */
    public Report bullet(Component chatContent, String consoleText) {
        chat.add(MM.parse("<" + Theme.C_ACCENT + ">" + Theme.SYMBOL_BULLET + "</"
                + Theme.C_ACCENT + "> ").append(chatContent));
        console.appendRaw(Theme.ANSI_ACCENT).appendRaw(Theme.SYMBOL_BULLET).appendRaw(" ")
                .appendRaw(Theme.ANSI_RESET).appendRaw(safe(consoleText)).appendRaw("\n");
        return this;
    }

    /** Append a one-off line where chat and console differ (e.g. clickable URL). */
    public Report line(Component chatLine, String consoleLine) {
        chat.add(chatLine);
        console.appendRaw(safe(consoleLine)).appendRaw("\n");
        return this;
    }

    /** Clickable URL in chat; key/value in console. */
    public Report link(String label, String url, String hover) {
        Component chatLine = MM.parse("<" + Theme.C_MUTED + ">" + safe(label) + ":</"
                + Theme.C_MUTED + "> ")
                .append(MM.parse("<" + Theme.C_ACCENT + "><underlined>" + escape(url)
                        + "</underlined></" + Theme.C_ACCENT + ">")
                        .clickEvent(ClickEvent.openUrl(url))
                        .hoverEvent(HoverEvent.showText(MM.muted(hover == null ? "" : hover))));
        chat.add(chatLine);
        console.appendRaw(Theme.ANSI_MUTED).appendRaw(safe(label)).appendRaw(":")
                .appendRaw(Theme.ANSI_RESET).appendRaw(" ")
                .appendRaw(Theme.ANSI_ACCENT).appendRaw(safe(url))
                .appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        return this;
    }

    public Report muted(String text) {
        chat.add(MM.muted(text));
        console.muted(text);
        return this;
    }

    public Report error(String text) {
        chat.add(MM.error(text));
        console.error(text);
        return this;
    }

    public Report warn(String text) {
        chat.add(MM.warn(text));
        console.warn(text);
        return this;
    }

    public Report success(String text) {
        chat.add(MM.ok(text));
        console.success(text);
        return this;
    }

    public Report blank() {
        chat.add(Component.empty());
        console.blankLine();
        return this;
    }

    /* ---------- list rendering ---------- */

    /**
     * One entry in a compact list view. Renders as:
     * <pre>
     *   ✔ name                    [show]
     *     description
     * </pre>
     */
    public Report listItem(String name, String description, Status status, String showCommand) {
        String chatColor = chatColor(status);
        String chatIcon = listIcon(status);

        Component header = MM.parse("<" + chatColor + "><bold>" + chatIcon + "</bold></"
                + chatColor + "> ")
                .append(MM.parse("<gradient:" + Theme.C_ACCENT + ":" + Theme.C_PRIMARY
                        + "><bold>" + escape(name) + "</bold></gradient>"));
        if (showCommand != null && !showCommand.isBlank()) {
            header = header.append(Component.space())
                    .append(actionComponent("show", showCommand, "Show details"));
        }
        chat.add(header);
        if (description != null && !description.isBlank()) {
            chat.add(MM.muted("  " + description));
        }

        String ansiColor = ansiColor(status);
        console.appendRaw(ansiColor).appendRaw(Theme.ANSI_BOLD).appendRaw(chatIcon)
                .appendRaw(Theme.ANSI_RESET).appendRaw(" ")
                .appendRaw(Theme.ANSI_BOLD).appendRaw(Theme.ANSI_ACCENT).appendRaw(safe(name))
                .appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        if (description != null && !description.isBlank()) {
            console.appendRaw(Theme.ANSI_MUTED).appendRaw("  ").appendRaw(description)
                    .appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        }
        return this;
    }

    /* ---------- actions / pager ---------- */

    public Report actions(Action... actions) {
        if (actions == null || actions.length == 0) {
            return this;
        }

        // chat: clickable [Label] buttons inline
        Component row = Component.empty();
        for (int i = 0; i < actions.length; i++) {
            if (i > 0) {
                row = row.append(Component.space());
            }
            Action a = actions[i];
            row = row.append(actionComponent(a.label(), a.command(), a.hover()));
        }
        chat.add(row);

        // console: "Actions" section with aligned command + hint
        console.blankLine();
        console.accent("Actions");
        int maxCmdLen = 0;
        for (Action a : actions) {
            maxCmdLen = Math.max(maxCmdLen, safe(a.command()).length());
        }
        for (Action a : actions) {
            String cmd = safe(a.command());
            int pad = Math.max(2, maxCmdLen - cmd.length() + 2);
            console.appendRaw("  ")
                    .appendRaw(Theme.ANSI_ACCENT).appendRaw(cmd).appendRaw(Theme.ANSI_RESET)
                    .appendRaw(" ".repeat(pad))
                    .appendRaw(Theme.ANSI_MUTED).appendRaw(safe(a.hover()))
                    .appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        }
        return this;
    }

    public Report pager(int page, int totalPages, String baseCommand) {
        // chat
        Component nav = Component.empty();
        if (page > 1) {
            nav = nav.append(MM.parse("<" + Theme.C_ACCENT + "><bold>"
                    + Theme.SYMBOL_ARROW_LEFT + "</bold></" + Theme.C_ACCENT + ">")
                    .clickEvent(ClickEvent.runCommand(baseCommand + " " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(MM.muted("Previous page"))));
        } else {
            nav = nav.append(MM.parse("<" + Theme.C_SEP + ">"
                    + Theme.SYMBOL_ARROW_LEFT + "</" + Theme.C_SEP + ">"));
        }
        nav = nav.append(MM.parse(" <" + Theme.C_MUTED + ">Page " + page + "/"
                + totalPages + "</" + Theme.C_MUTED + "> "));
        if (page < totalPages) {
            nav = nav.append(MM.parse("<" + Theme.C_ACCENT + "><bold>"
                    + Theme.SYMBOL_ARROW_RIGHT + "</bold></" + Theme.C_ACCENT + ">")
                    .clickEvent(ClickEvent.runCommand(baseCommand + " " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(MM.muted("Next page"))));
        } else {
            nav = nav.append(MM.parse("<" + Theme.C_SEP + ">"
                    + Theme.SYMBOL_ARROW_RIGHT + "</" + Theme.C_SEP + ">"));
        }
        chat.add(Component.empty());
        chat.add(nav);

        console.blankLine();
        console.appendRaw(Theme.ANSI_MUTED).appendRaw("Page ")
                .appendRaw(String.valueOf(page)).appendRaw("/")
                .appendRaw(String.valueOf(totalPages))
                .appendRaw(Theme.ANSI_RESET).appendRaw("\n");
        return this;
    }

    /* ---------- summary block ---------- */

    /**
     * Open a summary block at the end of a list. Adds a blank separator and a
     * section header. Call {@link #summary} repeatedly for each tally, then
     * {@link #pager} or {@link #send}.
     */
    public Report summarySection() {
        chat.add(Component.empty());
        chat.add(MM.accent("Summary"));
        console.blankLine();
        console.accent("Summary");
        return this;
    }

    public Report summary(String label, int count) {
        return kv(label, String.valueOf(count));
    }

    public Report summary(String label, int count, Status status) {
        return kv(label, String.valueOf(count), status);
    }

    /* ---------- terminal ---------- */

    public void send(CommandSource sender) {
        if (sender == null) {
            return;
        }
        if (new RenderContext(sender).isPlayer()) {
            ChatFrame frame = new ChatFrame(section);
            frame.lines(chat);
            frame.send(sender);
        } else {
            Log.info(console.build());
        }
    }

    /** Render only the chat side; console-side is dropped. For commands whose
     *  console rendering is special enough to stay outside the Report path. */
    public void sendChatOnly(CommandSource sender) {
        if (sender == null) {
            return;
        }
        if (new RenderContext(sender).isPlayer()) {
            ChatFrame frame = new ChatFrame(section);
            frame.lines(chat);
            frame.send(sender);
        }
    }

    /* ---------- internals ---------- */

    private static Component actionComponent(String label, String command, String hover) {
        return MM.parse("<" + Theme.C_ACCENT + "><bold>[" + label + "]</bold></"
                + Theme.C_ACCENT + ">")
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(MM.muted(hover == null ? "" : hover)));
    }

    private static String listIcon(Status status) {
        return switch (status) {
            case SUCCESS -> Theme.SYMBOL_CHECK;
            case ERROR -> Theme.SYMBOL_CROSS;
            default -> Theme.SYMBOL_BULLET;
        };
    }

    private static String chatColor(Status status) {
        return switch (status) {
            case SUCCESS -> Theme.C_SUCCESS;
            case WARN -> Theme.C_WARN;
            case ERROR -> Theme.C_ERROR;
            case ACCENT -> Theme.C_ACCENT;
            case NEUTRAL -> "white";
        };
    }

    private static String ansiColor(Status status) {
        return switch (status) {
            case SUCCESS -> Theme.ANSI_SUCCESS;
            case WARN -> Theme.ANSI_WARN;
            case ERROR -> Theme.ANSI_ERROR;
            case ACCENT -> Theme.ANSI_ACCENT;
            case NEUTRAL -> "";
        };
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("<", "\\<");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
