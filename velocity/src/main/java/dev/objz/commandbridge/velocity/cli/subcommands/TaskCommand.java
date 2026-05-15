package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.dispatch.ScheduleManager;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;
import dev.objz.commandbridge.velocity.ui.Report;
import dev.objz.commandbridge.velocity.ui.Report.Status;
import dev.objz.commandbridge.velocity.util.UserCache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TaskCommand {

    private static final String SECTION = "Tasks";
    private static final int PAGE_SIZE = 5;

    private final ScheduleManager scheduler;
    private final UserCache userCache;

    public TaskCommand(ScheduleManager scheduler, UserCache userCache) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.userCache = Objects.requireNonNull(userCache);
    }

    public void list(CommandSource sender, int requestedPage) {
        List<ScheduledTask> snapshot = new ArrayList<>(scheduler.tasks());
        snapshot.sort(Comparator.comparingLong(ScheduledTask::timestamp).reversed());

        Report report = Report.of(SECTION);
        if (snapshot.isEmpty()) {
            report.warn("No pending scheduled tasks").send(sender);
            return;
        }

        int totalPages = (int) Math.ceil((double) snapshot.size() / PAGE_SIZE);
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, snapshot.size());

        report.section("Summary")
                .summary("Pending", snapshot.size(), Status.ACCENT);

        report.section("Sections");
        long now = System.currentTimeMillis();
        for (int i = start; i < end; i++) {
            ScheduledTask task = snapshot.get(i);
            String shortId = task.id().toString().substring(0, 8);
            String player = userCache.displayName(task.playerUuid());
            String age = MM.formatDuration(Duration.ofMillis(Math.max(0, now - task.timestamp())));
            String script = task.scriptName() != null ? task.scriptName() : "unknown";
            String subline = script + " · " + player + " · " + age + " ago";
            report.listItem(shortId, subline, Status.ACCENT, null);
        }

        if (totalPages > 1) {
            report.pager(page, totalPages, "/cb task list");
        }

        report.send(sender);
    }

    public void clear(CommandSource sender, String idStr) {
        Optional<UUID> idOpt = parseId(idStr);
        if (idOpt.isEmpty()) {
            Report.of(SECTION).error("Invalid task id '" + idStr + "'").send(sender);
            return;
        }
        if (scheduler.removeById(idOpt.get())) {
            Report.of(SECTION).success("Cleared task '" + idStr + "'").send(sender);
        } else {
            Report.of(SECTION).error("Unknown task '" + idStr + "'").send(sender);
        }
    }

    private Optional<UUID> parseId(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(idStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
