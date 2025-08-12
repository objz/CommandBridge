package dev.objz.commandbridge.paper.command.inbound;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.objz.commandbridge.paper.Main;
import dev.objz.commandbridge.paper.core.Runtime;
import dev.objz.commandbridge.paper.utils.CommandUtils;
import dev.objz.commandbridge.paper.utils.SchedulerAdapter;
import dev.objz.commandbridge.core.json.MessageParser;
import dev.objz.commandbridge.core.Logger;

public class CommandHandler {
	private final Main plugin;
	private final Logger logger;

	public CommandHandler() {
		this.plugin = Main.getInstance();
		this.logger = Runtime.getInstance().getLogger();
	}

	public void dispatchCommand(String message) {
		MessageParser parser = new MessageParser(message);
		String serverId = Runtime.getInstance().getConfig().getKey("config.yml", "client-id");
		if (!parser.getBodyValueAsString("client").equals(serverId)) {
			logger.debug("Message not intended for this client: {}", serverId);
			return;
		}
		String command = parser.getBodyValueAsString("command");
		String target = parser.getBodyValueAsString("target");
		logger.info("Dispatching command '{}' for executor: {}", command, target);

		switch (target) {
			case "console" -> executeConsoleCommand(command);
			case "player" -> executePlayerCommand(parser, command);
			default -> logger.warn("Invalid target: {}", target);
		}
	}

	private void executeConsoleCommand(String command) {
		logger.debug("Executing command '{}' as console", command);

		CommandSender console = Bukkit.getConsoleSender();
		new SchedulerAdapter(plugin).run(() -> {
			if (CommandUtils.isCommandValid(command)) {
				logger.warn("Invalid command: {}", command);
				Runtime.getInstance().getClient().sendError("Invalid command: " + command);
				return;
			}

			boolean status = Bukkit.dispatchCommand(console, command);
			logResult("console", command, status);
		});
	}

	private void executePlayerCommand(MessageParser parser, String command) {
		logger.debug("Executing command '{}' as player", command);
		String uuidStr = parser.getBodyValueAsString("uuid");
		String name = parser.getBodyValueAsString("name");
		UUID uuid = UUID.fromString(uuidStr);

		Player player = Bukkit.getPlayer(uuid);

		if (player == null || !player.isOnline()) {
			logger.warn("Player '{}' not found or offline", name);
			Runtime.getInstance().getClient()
					.sendError("Player '" + name + "' not found or offline");
			return;
		}

		if (SchedulerAdapter.isFolia()) {
			player.getScheduler().execute(plugin, () -> {
				executeFoliaCommand(player, command);
			}, null, 0);
		} else {
			new SchedulerAdapter(plugin).run(() -> executeFoliaCommand(player, command));
		}

	}

	private void executeFoliaCommand(Player player, String command) {
		if (CommandUtils.isCommandValid(command)) {
			logger.warn("Invalid command: {}", command);
			Runtime.getInstance().getClient().sendError("Invalid command: " + command);
			return;
		}
		boolean status = Bukkit.dispatchCommand(player, command);
		logResult("player", command, status);
	}

	private void logResult(String target, String command, boolean status) {
		if (status) {
			logger.info("Successfully executed command '{}' as {}", command, target);
		} else {
			logger.warn("Failed to execute command '{}' as {}", command, target);
			Runtime.getInstance().getClient()
					.sendError("Failed to execute command '" + command + "' as " + target);
		}
	}

}
