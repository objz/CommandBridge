package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.util.MM;
//finished for now
public final class HelpCommand {
	public void execute(CommandSource sender) {
		MM.msg()
				.space()
				.superTitle("CommandBridge")
				.cmdLine("/cb help", "show this help")
				.cmdLine("/cb reload", "reload all")
				.cmdLine("/cb scripts", "list all scripts")
				.cmdLine("/cb list", "list connected clients")
				.cmdLine("/cb ping", "ping all clients")
				.cmdLine("/cb debug", "toggle debug mode")
				.cmdLine("/cb dump", "dump your config and scripts")
				.send(sender);
	}
}
