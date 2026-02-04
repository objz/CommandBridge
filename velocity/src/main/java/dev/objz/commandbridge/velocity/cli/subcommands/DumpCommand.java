package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.chat.ChatFrame;
import dev.objz.commandbridge.velocity.ui.chat.ChatLayout;
import dev.objz.commandbridge.velocity.ui.cli.CliOutput;
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
		Component clients = MM.parse("<" + Theme.C_MUTED + ">Clients</" + Theme.C_MUTED + "> <" + Theme.C_ACCENT + ">" + clientCount + "</" + Theme.C_ACCENT + ">");
		Component todo = MM.warn("TODO: detailed registration dump");
		int width = Math.max(ChatLayout.titleWidth("Dump"), ChatLayout.visibleLength(clients));
		width = Math.max(width, ChatLayout.visibleLength(todo));
		width = Math.max(width, ChatLayout.DEFAULT_WIDTH_PX);
		ChatFrame frame = new ChatFrame("Dump").width(width);
		frame.line(clients);
		frame.line(todo);
		frame.send(ctx.source());
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
