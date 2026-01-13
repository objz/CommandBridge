package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.util.MM;

import java.util.ArrayList;
import java.util.List;

//!TODO: Paginate if too many clients are connected
//list if its a velocity or backend client
public final class ListCommand {
	private final SessionHub sessions;

	public ListCommand(SessionHub sessions) {
		this.sessions = sessions;
	}

	public void execute(CommandSource sender) {
		List<ClientSession> authenticated = new ArrayList<>();
		
		for (ClientSession s : sessions) {
			if (s.status() == AuthStatus.AUTH_OK && s.ch() != null && s.ch().isOpen()) {
				authenticated.add(s);
			}
		}

		if (authenticated.isEmpty()) {
			MM.msg().space().line(MM.warn("No authenticated clients connected")).send(sender);
			return;
		}

		var m = MM.msg()
				.space()
				.header("Connected Clients (" + authenticated.size() + ")");

		for (var s : authenticated) {
			String id = s.id() != null ? s.id() : "unknown";
			String address = s.ch() != null && s.ch().getSourceAddress() != null
					? s.ch().getSourceAddress().toString()
					: "unknown";

			m.item("<green>[OK]</green> <white>" + id + "</white> <gray>" + address + "</gray>");
		}

		m.space()
				.line(MM.muted("Tip: Use ").append(MM.cmd("/cb ping"))
						.append(MM.muted(" to check client latency")))
				.send(sender);
	}
}
