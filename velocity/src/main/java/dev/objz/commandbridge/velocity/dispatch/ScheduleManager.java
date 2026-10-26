package dev.objz.commandbridge.velocity.dispatch;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;
import dev.objz.commandbridge.velocity.util.PlayerTracker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class ScheduleManager {

    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+}");

    private final ProxyServer proxy;
    private final ScriptManager scriptManager;
    private final String localVelocityId;
    private final Duration expireAfter;

    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Path storagePath;

    private volatile Consumer<ExecutionContext> executionCallback;

    public ScheduleManager(ProxyServer proxy, Object plugin, Path dataDir,
            ScriptManager scriptManager, PlayerTracker playerTracker,
            String localVelocityId, Duration expireAfter) {
        this.proxy = proxy;
        this.scriptManager = scriptManager;
        this.localVelocityId = localVelocityId;
        this.expireAfter = Objects.requireNonNull(expireAfter);
        this.storagePath = dataDir.resolve("data").resolve("tasks.json");

        loadTasks();

        proxy.getScheduler().buildTask(plugin, () -> {
            pruneExpired();
            saveTasks();
        })
                .repeat(5, TimeUnit.MINUTES)
                .schedule();

        playerTracker.onPlayerJoin(this::onPlayerJoin);
    }

    public void setExecutionCallback(Consumer<ExecutionContext> callback) {
        this.executionCallback = callback;
    }

    public Collection<ScheduledTask> tasks() {
        return Collections.unmodifiableCollection(new ArrayList<>(tasks.values()));
    }

    public int size() {
        return tasks.size();
    }

    public int clearAll() {
        int removed = tasks.size();
        if (removed == 0) {
            return 0;
        }
        tasks.clear();
        saveTasks();
        return removed;
    }

    public Optional<ScheduledTask> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tasks.get(id));
    }

    public boolean removeById(UUID id) {
        if (id == null) {
            return false;
        }
        if (tasks.remove(id) != null) {
            saveTasks();
            return true;
        }
        return false;
    }

    public void queueTask(ExecutionContext ctx, CmdMapping cmd, int index) {
        UUID playerUuid = ctx.getPlayerUuid();
        if (playerUuid == null) {
            Log.warn("Cannot schedule task for non-player source");
            return;
        }

        UUID taskId = UUID.randomUUID();
        ScheduledTask task = new ScheduledTask(
                taskId,
                playerUuid,
                ctx.script().name(),
                cmd,
                ctx.arguments(),
                index,
                System.currentTimeMillis(),
                ctx.script());

        tasks.put(taskId, task);
        saveTasks();

        Log.debug("Queued task {} for player {} (waiting for player to join target)",
                taskId, playerUuid);
    }

    private void onPlayerJoin(String clientId, UUID playerUuid) {
        processQueue(clientId, playerUuid);
    }

    private void processQueue(String clientId, UUID playerUuid) {
        Consumer<ExecutionContext> callback = executionCallback;
        if (callback == null) {
            return;
        }
        for (ScheduledTask task : tasks.values()) {

            if (!task.playerUuid().equals(playerUuid)) {
                continue;
            }

            if (!isTargetingClient(task.commandMapping(), clientId)) {
                continue;
            }

            if (!tasks.remove(task.id(), task)) {
                continue;
            }
            // Consume before dispatch: a synchronous failure may already have executed part of the chain.
            saveTasks();

            Log.debug("Resuming task {} for player {} on client {}",
                    task.id(), playerUuid, clientId);

            try {
                ExecutionContext ctx = isExpired(task) ? null : reconstructContext(playerUuid, task);
                if (ctx != null) {
                    callback.accept(ctx);
                }
            } catch (Exception e) {
                Log.error(e, "Scheduled task '{}' failed and was consumed; it will not be retried", task.id());
            }
        }
    }

    private boolean isTargetingClient(CmdMapping mapping, String clientId) {
        if (mapping == null || mapping.execute() == null)
            return false;

        return mapping.execute().stream().anyMatch(target -> {
            if (target.location() == Location.VELOCITY) {
                return target.id().equalsIgnoreCase(clientId)
                        || (target.id().equalsIgnoreCase(localVelocityId)
                                && clientId.equalsIgnoreCase(localVelocityId));
            }
            return target.id().equalsIgnoreCase(clientId);
        });
    }

    private ExecutionContext reconstructContext(UUID playerUuid, ScheduledTask task) {
        // The continuation was authorized at enqueue; never splice in a revised script.
        Script script = scriptManager.enabled().stream()
                .filter(s -> s.enabled() && s.name().equals(task.scriptName()) && s.equals(task.authorizedScript()))
                .findFirst()
                .orElse(null);

        if (script == null) {
            Log.warn("Script '{}' for scheduled task changed, disabled, invalid or lacks an authorized revision; discarding task",
                    task.scriptName());
            return null;
        }

        if (task.commandIndex() < 0 || task.commandIndex() >= script.commands().size()) {
            Log.warn("Scheduled task '{}' has an invalid command index; discarding task", task.id());
            return null;
        }

        Player player = proxy.getPlayer(playerUuid).orElse(null);

        return new ExecutionContext(
                null,
                null,
                player,
                playerUuid,
                script,
                task.arguments(),
                task.commandMapping(),
                task.commandIndex());
    }

    private synchronized void loadTasks() {
        Log.debug("Loading scheduled tasks from '{}'", storagePath);
        if (Files.notExists(storagePath))
            return;
        {
            List<ScheduledTask> loaded = TaskStore.load(storagePath);
            int expired = 0;
            int unresolved = 0;
            for (ScheduledTask t : loaded) {
                if (isExpired(t)) {
                    expired++;
                    continue;
                }
                if (hasUnresolvedTargets(t)) {
                    Log.warn("Dropping scheduled task '{}' with unresolved placeholder in execute target",
                            t.id());
                    unresolved++;
                    continue;
                }
                tasks.put(t.id(), t);
            }
            int dropped = expired + unresolved;
            if (dropped > 0) {
                Log.info("Dropped '{}' stale scheduled tasks during load (expired={}, unresolved={})",
                        dropped, expired, unresolved);
            }
            Log.success(true, "Loaded '{}' pending tasks", tasks.size());
        }
    }

    private synchronized void saveTasks() {
        TaskStore.save(storagePath, tasks.values());
    }

    private void pruneExpired() {
        if (expireAfter.isZero() || expireAfter.isNegative()) {
            return;
        }
        int before = tasks.size();
        tasks.values().removeIf(this::isExpired);
        int removed = before - tasks.size();
        if (removed > 0) {
            Log.debug("Pruned '{}' expired scheduled tasks", removed);
        }
    }

    private boolean isExpired(ScheduledTask task) {
        if (expireAfter.isZero() || expireAfter.isNegative()) {
            return false;
        }
        return System.currentTimeMillis() - task.timestamp() > expireAfter.toMillis();
    }

    private static boolean hasUnresolvedTargets(ScheduledTask task) {
        CmdMapping cmd = task.commandMapping();
        if (cmd == null || cmd.execute() == null) {
            return false;
        }
        return cmd.execute().stream()
                .anyMatch(t -> t != null && t.id() != null
                        && UNRESOLVED_PLACEHOLDER.matcher(t.id()).find());
    }
}
