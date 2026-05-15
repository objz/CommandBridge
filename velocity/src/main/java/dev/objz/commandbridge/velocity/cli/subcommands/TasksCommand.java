package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.dispatch.ScheduleManager;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
import dev.objz.commandbridge.velocity.ui.cli.CliTable;
import dev.objz.commandbridge.velocity.util.UserCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TasksCommand extends AbstractCliCommand {

    private static final int PAGE_SIZE = 5;
    private static final int COMMAND_PREVIEW_MAX = 60;

    private final ProxyServer proxy;
    private final ScheduleManager scheduler;
    private final UserCache userCache;

    public TasksCommand(ProxyServer proxy, ScheduleManager scheduler, UserCache userCache) {
        this.proxy = Objects.requireNonNull(proxy);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.userCache = Objects.requireNonNull(userCache);
    }

    public void list(CommandSource sender, int page) {
        RenderContext ctx = new RenderContext(sender);
        List<ScheduledTask> snapshot = new ArrayList<>(scheduler.tasks());
        snapshot.sort(Comparator.comparingLong(ScheduledTask::timestamp).reversed());

        if (ctx.isPlayer()) {
            renderChatList(ctx, snapshot, page);
        } else {
            renderConsoleList(snapshot);
        }
    }

    public void clearAll(CommandSource sender) {
        int removed = scheduler.clearAll();
        RenderContext ctx = new RenderContext(sender);
        if (ctx.isPlayer()) {
            ChatFrame frame = new ChatFrame("Tasks");
            if (removed == 0) {
                frame.line(MM.warn("No pending tasks to clear"));
            } else {
                frame.line(MM.ok("Cleared " + removed + " pending task" + plural(removed)));
            }
            frame.send(sender);
            return;
        }
        CliOutput output = cli("Tasks");
        if (removed == 0) {
            output.warn("No pending tasks to clear");
        } else {
            output.success("Cleared " + removed + " pending task" + plural(removed));
        }
        log(output);
    }

    public void clearByPlayer(CommandSource sender, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            sendError(sender, "Player name required");
            return;
        }
        userCache.resolve(playerName).whenComplete((uuid, error) -> {
            if (error != null) {
                sendError(sender, "Failed to resolve '" + playerName + "': " + error.getMessage());
                return;
            }
            if (uuid == null) {
                sendError(sender, "Unknown player '" + playerName + "'");
                return;
            }
            int removed = scheduler.clearByPlayer(uuid);
            sendClearResult(sender, playerName, removed);
        });
    }

    private void sendClearResult(CommandSource sender, String playerName, int removed) {
        RenderContext ctx = new RenderContext(sender);
        if (ctx.isPlayer()) {
            ChatFrame frame = new ChatFrame("Tasks");
            if (removed == 0) {
                frame.line(MM.warn("No pending tasks for '" + playerName + "'"));
            } else {
                frame.line(MM.ok("Cleared " + removed + " task" + plural(removed)
                        + " for '" + playerName + "'"));
            }
            frame.send(sender);
            return;
        }
        CliOutput output = cli("Tasks");
        if (removed == 0) {
            output.warn("No pending tasks for '" + playerName + "'");
        } else {
            output.success("Cleared " + removed + " task" + plural(removed)
                    + " for '" + playerName + "'");
        }
        log(output);
    }

    private void sendError(CommandSource sender, String message) {
        RenderContext ctx = new RenderContext(sender);
        if (ctx.isPlayer()) {
            ChatFrame frame = new ChatFrame("Tasks");
            frame.line(MM.error(message));
            frame.send(sender);
            return;
        }
        CliOutput output = cli("Tasks");
        output.error(message);
        log(output);
    }

    private void renderChatList(RenderContext ctx, List<ScheduledTask> tasks, int requestedPage) {
        if (tasks.isEmpty()) {
            ChatFrame frame = new ChatFrame("Tasks");
            frame.line(MM.warn("No pending scheduled tasks"));
            frame.send(ctx.source());
            return;
        }

        int totalPages = (int) Math.ceil((double) tasks.size() / PAGE_SIZE);
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, tasks.size());

        List<Component> lines = new ArrayList<>();
        lines.add(MM.parse("<" + Theme.C_MUTED + ">Pending tasks</" + Theme.C_MUTED + "> <"
                + Theme.C_ACCENT + ">" + tasks.size() + "</" + Theme.C_ACCENT + ">"));
        lines.add(Component.empty());

        long now = System.currentTimeMillis();
        for (int i = start; i < end; i++) {
            ScheduledTask task = tasks.get(i);
            String player = displayPlayer(task.playerUuid());
            String targets = formatTargets(task.commandMapping());
            String commandPreview = previewCommand(task.commandMapping());
            String age = MM.formatDuration(Duration.ofMillis(Math.max(0, now - task.timestamp())));

            Component header = MM.parse("<" + Theme.C_ACCENT + ">•</" + Theme.C_ACCENT + "> ")
                    .append(MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT
                            + "><bold>" + escape(player) + "</bold></gradient>"))
                    .append(MM.parse(" <" + Theme.C_MUTED + ">("
                            + escape(task.scriptName()) + ")</" + Theme.C_MUTED + ">"));
            Component cmdLine = MM.parse("<" + Theme.C_MUTED + ">  Command:</"
                    + Theme.C_MUTED + "> <white>" + escape(commandPreview) + "</white>");
            Component targetsLine = MM.parse("<" + Theme.C_MUTED + ">  Targets:</"
                    + Theme.C_MUTED + "> <white>" + escape(targets) + "</white>");
            Component ageLine = MM.parse("<" + Theme.C_MUTED + ">  Age:</"
                    + Theme.C_MUTED + "> <white>" + age + "</white>");

            lines.add(header);
            lines.add(cmdLine);
            lines.add(targetsLine);
            lines.add(ageLine);
            if (i < end - 1) {
                lines.add(Component.empty());
            }
        }

        if (totalPages > 1) {
            lines.add(Component.empty());
            lines.add(buildPager(page, totalPages));
        }

        ChatFrame frame = new ChatFrame("Tasks");
        frame.lines(lines);
        frame.send(ctx.source());
    }

    private Component buildPager(int page, int totalPages) {
        Component nav = Component.empty();
        if (page > 1) {
            nav = nav.append(MM.parse("<" + Theme.C_ACCENT + "><bold>"
                    + Theme.SYMBOL_ARROW_LEFT + "</bold></" + Theme.C_ACCENT + ">")
                    .clickEvent(ClickEvent.runCommand("/cb tasks list " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(MM.parse("<" + Theme.C_MUTED
                            + ">Previous page</" + Theme.C_MUTED + ">"))));
        } else {
            nav = nav.append(MM.parse("<" + Theme.C_SEP + ">"
                    + Theme.SYMBOL_ARROW_LEFT + "</" + Theme.C_SEP + ">"));
        }
        nav = nav.append(MM.parse(" <" + Theme.C_MUTED + ">Page " + page + "/"
                + totalPages + "</" + Theme.C_MUTED + "> "));
        if (page < totalPages) {
            nav = nav.append(MM.parse("<" + Theme.C_ACCENT + "><bold>"
                    + Theme.SYMBOL_ARROW_RIGHT + "</bold></" + Theme.C_ACCENT + ">")
                    .clickEvent(ClickEvent.runCommand("/cb tasks list " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(MM.parse("<" + Theme.C_MUTED
                            + ">Next page</" + Theme.C_MUTED + ">"))));
        } else {
            nav = nav.append(MM.parse("<" + Theme.C_SEP + ">"
                    + Theme.SYMBOL_ARROW_RIGHT + "</" + Theme.C_SEP + ">"));
        }
        return nav;
    }

    private void renderConsoleList(List<ScheduledTask> tasks) {
        CliOutput output = cli("Tasks");
        if (tasks.isEmpty()) {
            output.warn("No pending scheduled tasks");
            log(output);
            return;
        }

        long now = System.currentTimeMillis();
        CliTable table = new CliTable()
                .width(output.width())
                .addColumn("Player", CliTable.Align.LEFT, 2, 12)
                .addColumn("Script", CliTable.Align.LEFT, 2, 10)
                .addColumn("Command", CliTable.Align.LEFT, 5, 24)
                .addColumn("Targets", CliTable.Align.LEFT, 3, 14)
                .addColumn("Age", CliTable.Align.RIGHT, 1, 6);

        for (ScheduledTask task : tasks) {
            String player = displayPlayer(task.playerUuid());
            String script = task.scriptName() != null ? task.scriptName() : "unknown";
            String command = previewCommand(task.commandMapping());
            String targets = formatTargets(task.commandMapping());
            String age = MM.formatDuration(Duration.ofMillis(Math.max(0, now - task.timestamp())));
            table.addRow(player, script, command, targets, age);
        }

        output.appendRaw(table.render());
        output.blankLine();
        output.muted("Total: " + tasks.size());
        log(output);
    }

    private String displayPlayer(UUID uuid) {
        if (uuid == null) {
            return "unknown";
        }
        Player online = proxy.getPlayer(uuid).orElse(null);
        if (online != null) {
            return online.getUsername();
        }
        return userCache.nameOf(uuid).orElseGet(() -> uuid.toString().substring(0, 8));
    }

    private static String formatTargets(CmdMapping cmd) {
        if (cmd == null || cmd.execute() == null || cmd.execute().isEmpty()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cmd.execute().size(); i++) {
            IdMapping target = cmd.execute().get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(target.id() != null ? target.id() : "?");
        }
        return sb.toString();
    }

    private static String previewCommand(CmdMapping cmd) {
        if (cmd == null || cmd.command() == null) {
            return "(none)";
        }
        String command = cmd.command();
        if (command.length() <= COMMAND_PREVIEW_MAX) {
            return command;
        }
        return command.substring(0, COMMAND_PREVIEW_MAX - 1) + "…";
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("<", "\\<");
    }

    private static String plural(int n) {
        return n == 1 ? "" : "s";
    }
}
