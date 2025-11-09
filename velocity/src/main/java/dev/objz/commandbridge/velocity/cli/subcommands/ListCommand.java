package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.MM;

import java.util.ArrayList;
import java.util.List;

//TODO
public final class ListCommand {
	private final SessionHub sessions;

	public ListCommand(SessionHub sessions) {
		this.sessions = sessions;
	}

	public void execute(CommandSource sender) {
		List<ClientSession> list = new ArrayList<>();
		for (ClientSession s : sessions)
			list.add(s);

		if (list.isEmpty()) {
			MM.msg().space().line(MM.warn("[WARN] No clients connected")).send(sender);
			return;
		}

		var m = MM.msg()
				.space()
				.header("Clients (" + list.size() + ")");

		for (var s : list) {
			String id = s.id() != null ? s.id() : "unknown";
			boolean ok = s.status() == AuthStatus.AUTH_OK;
			String badge = ok ? "<green>[OK]</green>" : "<red>[NOAUTH]</red>";
			m.item(badge + " <white>" + id + "</white> <gray>(" + s.status() + ")</gray>");
		}

		m.send(sender);
	}
}
