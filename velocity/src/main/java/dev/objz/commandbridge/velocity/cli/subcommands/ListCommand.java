package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.BarBuilder;
import dev.objz.commandbridge.velocity.util.MM;

import java.util.ArrayList;
import java.util.List;

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
			MM.msg().space().line(MM.warn("No clients connected")).send(sender);
			return;
		}

		int authenticated = 0;
		int connected = 0;
		int closed = 0;

		for (ClientSession s : list) {
			if (s.status() == AuthStatus.AUTH_OK)
				authenticated++;
			if (s.ch() != null && s.ch().isOpen())
				connected++;
			else
				closed++;
		}

		double greenPct = authenticated / (double) list.size();
		double yellowPct = (connected - authenticated) / (double) list.size();
		double redPct = closed / (double) list.size();

		String bar = BarBuilder.create(110)
				.add("green", greenPct)
				.add("yellow", yellowPct)
				.add("red", redPct)
				.build();

		var m = MM.msg()
				.space()
				.header("Clients (" + list.size() + ")")
				.line(MM.parse(bar))
				.line(MM.kv("authenticated", "<green>" + authenticated + "</green>")
						.append(MM.sep())
						.append(MM.kv("connected", "<yellow>" + connected + "</yellow>"))
						.append(MM.sep())
						.append(MM.kv("disconnected", "<red>" + closed + "</red>")))
				.space();

		for (var s : list) {
			String id = s.id() != null ? s.id() : "unknown";
			boolean authenticated = s.status() == AuthStatus.AUTH_OK;
			boolean open = s.ch() != null && s.ch().isOpen();

			String badge;
			if (authenticated && open) {
				badge = "<green>[OK]</green>";
			} else if (open) {
				badge = "<yellow>[NOAUTH]</yellow>";
			} else {
				badge = "<red>[CLOSED]</red>";
			}

			String address = s.ch() != null && s.ch().getSourceAddress() != null
					? s.ch().getSourceAddress().toString()
					: "unknown";

			m.item(badge + " <white>" + id + "</white> <gray>" + address + "</gray>");
		}

		m.space()
				.line(MM.muted("Tip: ").append(MM.cmd("/cb inspect <client>"))
						.append(MM.muted(" for detailed info")))
				.send(sender);
	}
}
