package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.util.MM;

public final class DebugCommand {
	public void execute(CommandSource sender) {
		boolean newState = !Log.isDebug();
		Log.setDebug(newState);
		MM.msg()
				.space()
				.line(MM.desc("Debug is now ")
						.append(newState ? MM.ok("ENABLED") : MM.warn("DISABLED")))
				.send(sender);
	}
}
