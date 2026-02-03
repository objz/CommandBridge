package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.ui.CliOutput;
import dev.objz.commandbridge.velocity.ui.RenderContext;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class DumpCommand extends AbstractCliCommand {

	private final SessionHub sessions;

	public DumpCommand(RegistrationManager registrations, SessionHub sessions) {
		this.sessions = sessions;
	}

	public void execute(CommandSource sender) {
		List<ClientSession> list = new ArrayList<>();
		for (ClientSession s : sessions)
			list.add(s);

		RenderContext ctx = new RenderContext(sender);
		
		if (ctx.isPlayer()) {
			renderChat(ctx, list.size());
		} else {
			renderConsole(list.size());
		}
	}
	
	private void renderChat(RenderContext ctx, int clientCount) {
		Component message = Component.text()
				.append(Component.text("Dump", net.kyori.adventure.text.format.TextColor.fromHexString(Theme.C_PRIMARY)))
				.append(Component.newline())
				.append(Component.text("Clients: " + clientCount, net.kyori.adventure.text.format.TextColor.fromHexString(Theme.C_MUTED)))
				.append(Component.newline())
				.append(Component.text("TODO: detailed registration dump", net.kyori.adventure.text.format.TextColor.fromHexString(Theme.C_WARN)))
				.build();
		ctx.source().sendMessage(message);
	}
	
	private void renderConsole(int clientCount) {
		CliOutput output = cli("Dump");
		output.appendRaw(Theme.ANSI_MUTED).appendRaw("Clients: ").appendRaw(Theme.ANSI_RESET);
		output.appendRaw(Theme.ANSI_ACCENT).appendRaw(String.valueOf(clientCount)).appendRaw(Theme.ANSI_RESET);
		output.appendRaw("\n\n");
		output.warn("TODO: detailed registration dump");
		log(output);
	}
}
