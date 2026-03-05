package dev.objz.commandbridge.velocity.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ScheduleManager {

    private final ProxyServer proxy;
    private final ScriptManager scriptManager;
    private final String localVelocityId;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final File storageFile;

    private Consumer<ExecutionContext> executionCallback;

    public ScheduleManager(ProxyServer proxy, Object plugin, Path dataDir,
            ScriptManager scriptManager, PlayerTracker playerTracker,
            String localVelocityId) {
        this.proxy = proxy;
        this.scriptManager = scriptManager;
        this.localVelocityId = localVelocityId;
        this.storageFile = dataDir.resolve("data").resolve("tasks.json").toFile();

        loadTasks();

        proxy.getScheduler().buildTask(plugin, this::saveTasks)
                .repeat(5, TimeUnit.MINUTES)
                .schedule();

        playerTracker.onPlayerJoin(this::onPlayerJoin);
    }

    public void setExecutionCallback(Consumer<ExecutionContext> callback) {
        this.executionCallback = callback;
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
        if (!storageFile.exists())
            return;
        try {
            List<ScheduledTask> loaded = mapper.readValue(storageFile,
                    new TypeReference<List<ScheduledTask>>() {
                    });
            for (ScheduledTask t : loaded) {
                tasks.put(t.id(), t);
            }
            Log.success(true, "Loaded '{}' pending tasks", tasks.size());
        } catch (IOException e) {
            Log.error("Failed to load scheduled tasks: " + e.getMessage());
        }
    }

    private synchronized void saveTasks() {
        try {
            storageFile.getParentFile().mkdirs();
            mapper.writeValue(storageFile, new ArrayList<>(tasks.values()));
        } catch (IOException e) {
            Log.error("Failed to save scheduled tasks: " + e.getMessage());
        }
    }
}
