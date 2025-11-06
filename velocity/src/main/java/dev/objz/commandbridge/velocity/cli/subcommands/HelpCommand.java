package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.util.MM;

public final class HelpCommand {
	public void execute(CommandSource sender) {
		MM.msg()
				.header("CommandBridge")
				.cmdLine("/cb help", "show this help")
				.cmdLine("/cb reload", "reload config & registrations")
				.cmdLine("/cb scripts", "list loaded scripts")
				.cmdLine("/cb list", "list connected clients")
				.cmdLine("/cb inspect <id>", "inspect client details")
				.cmdLine("/cb ping", "websocket pings for all clients")
				.cmdLine("/cb debug", "toggle debug mode")
				.cmdLine("/cb dump", "summary dump (WIP)")
				.send(sender);
	}
}
