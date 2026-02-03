package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.velocity.ui.CliOutput;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;

public class DebugCommand extends AbstractCliCommand {
	public void execute(CommandSource sender) {
		boolean newState = !Log.isDebug();
		Log.setDebug(newState);
		
		RenderContext ctx = new RenderContext(sender);
		
		if (ctx.isPlayer()) {
			renderChat(ctx, newState);
		} else {
			renderConsole(newState);
		}
	}
	
	private void renderChat(RenderContext ctx, boolean enabled) {
		Component message = Component.text()
				.append(Component.text("Debug Mode ", net.kyori.adventure.text.format.TextColor.fromHexString(Theme.C_MUTED)))
				.append(enabled 
						? Component.text("ENABLED", net.kyori.adventure.text.format.TextColor.fromHexString(Theme.C_SUCCESS))
						: Component.text("DISABLED", net.kyori.adventure.text.format.TextColor.fromHexString(Theme.C_WARN)))
				.build();
		ctx.source().sendMessage(message);
	}
	
	private void renderConsole(boolean enabled) {
		CliOutput output = cli("Debug Mode");
		String status = enabled ? "ENABLED" : "DISABLED";
		String statusColor = enabled ? Theme.ANSI_SUCCESS : Theme.ANSI_WARN;
		
		output.appendRaw("Debug is now ");
		output.appendRaw(statusColor).appendRaw(Theme.ANSI_BOLD);
		output.appendRaw(status);
		output.appendRaw(Theme.ANSI_RESET).appendRaw("\n");
		
		log(output);
	}
}
