package dev.objz.commandbridge.velocity.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;
import dev.objz.commandbridge.velocity.util.PlayerTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private Consumer<ExecutionContext> executionCallback;

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

    public int clearByPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        int before = tasks.size();
        tasks.values().removeIf(t -> playerUuid.equals(t.playerUuid()));
        int removed = before - tasks.size();
        if (removed > 0) {
            saveTasks();
        }
        return removed;
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
                System.currentTimeMillis());

        tasks.put(taskId, task);
        saveTasks();

        Log.debug("Queued task {} for player {} (waiting for player to join target)",
                taskId, playerUuid);
    }

    private void onPlayerJoin(String clientId, UUID playerUuid) {
        processQueue(clientId, playerUuid);
    }

    private void processQueue(String clientId, UUID playerUuid) {
        Iterator<Map.Entry<UUID, ScheduledTask>> it = tasks.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, ScheduledTask> entry = it.next();
            ScheduledTask task = entry.getValue();

            if (!task.playerUuid().equals(playerUuid)) {
                continue;
            }

            if (!isTargetingClient(task.commandMapping(), clientId)) {
                continue;
            }

            Log.debug("Resuming task {} for player {} on client {}",
                    task.id(), playerUuid, clientId);

            ExecutionContext ctx = reconstructContext(playerUuid, task);
            if (ctx != null && executionCallback != null) {
                executionCallback.accept(ctx);
            }

            it.remove();
        }
        saveTasks();
    }

    private boolean isTargetingClient(CmdMapping mapping, String clientId) {
        if (mapping.execute() == null)
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
        Script script = scriptManager.loaded().stream()
                .filter(s -> s.name().equals(task.scriptName()))
                .findFirst()
                .orElse(null);

        if (script == null) {
            Log.warn("Script '{}' for scheduled task missing, discarding task", task.scriptName());
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
        try {
            List<ScheduledTask> loaded = Envelope.MAPPER.readValue(storagePath.toFile(),
                    new TypeReference<List<ScheduledTask>>() {
                    });
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
        } catch (IOException e) {
            Log.error("Failed to load scheduled tasks: {}", e.getMessage());
        }
    }

    private synchronized void saveTasks() {
        try {
            Files.createDirectories(storagePath.getParent());
            Envelope.MAPPER.writeValue(storagePath.toFile(), new ArrayList<>(tasks.values()));
        } catch (IOException e) {
            Log.error("Failed to save scheduled tasks: {}", e.getMessage());
        }
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
