package dev.objz.commandbridge.velocity.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ScheduledTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ScheduleManager {

	private final ScriptManager scriptManager;
	private final ObjectMapper mapper = new ObjectMapper();
	private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
	private final File storageFile;

	private Consumer<ExecutionContext> executionCallback;

	public ScheduleManager(ProxyServer proxy, Path dataDir, ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
		this.storageFile = dataDir.resolve("tasks.json").toFile();

		loadTasks();

		proxy.getScheduler().buildTask(proxy, this::saveTasks)
				.repeat(5, TimeUnit.MINUTES)
				.schedule();
	}

	public void setExecutionCallback(Consumer<ExecutionContext> callback) {
		this.executionCallback = callback;
	}

	public void queueTask(ExecutionContext ctx, CmdMapping cmd, int index) {
		if (ctx.source() instanceof Player player) {
			UUID taskId = UUID.randomUUID();
			ScheduledTask task = new ScheduledTask(
					taskId,
					player.getUniqueId(),
					ctx.script().name(),
					cmd,
					ctx.arguments(),
					index,
					System.currentTimeMillis());

			tasks.put(taskId, task);
			saveTasks();

			Log.debug("Queued task {} for player {} (waiting for connection)", taskId,
					player.getUsername());
		} else {
			Log.warn("Cannot schedule task for non-player source");
		}
	}

	@Subscribe
	public void onServerConnected(ServerConnectedEvent event) {
		Player player = event.getPlayer();
		String serverName = event.getServer().getServerInfo().getName();

		processQueue(player, serverName);
	}

	private void processQueue(Player player, String currentServerId) {
		Iterator<Map.Entry<UUID, ScheduledTask>> it = tasks.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<UUID, ScheduledTask> entry = it.next();
			ScheduledTask task = entry.getValue();

			if (!task.playerUuid().equals(player.getUniqueId())) {
				continue;
			}

			boolean isTargetServer = isTargetingServer(task.commandMapping(), currentServerId);

			if (isTargetServer) {
				Log.debug("Resuming task {} for player {} on server {}", task.id(),
						player.getUsername(), currentServerId);

				ExecutionContext ctx = reconstructContext(player, task);
				if (ctx != null && executionCallback != null) {
					executionCallback.accept(ctx);
				}

				it.remove();
			}
		}
		saveTasks();
	}

	private boolean isTargetingServer(CmdMapping mapping, String serverId) {
		if (mapping.execute() == null)
			return false;

		return mapping.execute().stream()
				.anyMatch(idMapping -> idMapping
						.location() == dev.objz.commandbridge.scripting.model.enums.Location.BACKEND
						&&
						idMapping.id().equalsIgnoreCase(serverId));
	}

	private ExecutionContext reconstructContext(Player player, ScheduledTask task) {
		Script script = scriptManager.loaded().stream()
				.filter(s -> s.name().equals(task.scriptName()))
				.findFirst()
				.orElse(null);

		if (script == null) {
			Log.warn("Script '{}' for scheduled task missing, discarding task", task.scriptName());
			return null;
		}

		return new ExecutionContext(
				null,
				null,
				player,
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
			Log.info("Loaded {} pending tasks", tasks.size());
		} catch (IOException e) {
			Log.error("Failed to load scheduled tasks: " + e.getMessage());
		}
	}

	private synchronized void saveTasks() {
		try {
			mapper.writeValue(storageFile, new ArrayList<>(tasks.values()));
		} catch (IOException e) {
			Log.error("Failed to save scheduled tasks: " + e.getMessage());
		}
	}
}
