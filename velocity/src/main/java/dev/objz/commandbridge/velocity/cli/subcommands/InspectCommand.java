package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.MM;

//TODO
public final class InspectCommand {
	private final SessionHub sessions;

	public InspectCommand(SessionHub sessions) {
		this.sessions = sessions;
	}

	public void execute(CommandSource sender, String id) {
		ClientSession target = null;
		for (ClientSession s : sessions) {
			if (s.id() != null && s.id().equalsIgnoreCase(id)) {
				target = s;
				break;
			}
		}
		if (target == null) {
			MM.msg().space().line(MM.error("[ERROR] Client '" + id + "' not found")).send(sender);
			return;
		}

		boolean open = target.ch() != null && target.ch().isOpen();
		String address = target.ch() != null && target.ch().getSourceAddress() != null
				? target.ch().getSourceAddress().toString()
				: "unknown";
		String protocol = target.ch() != null ? ("HTTP/" + target.ch().getSubProtocol()) : "unknown";

		MM.msg()
				.space()
				.header("Client")
				.kv("id", target.id())
				.kv("status", target.status() == AuthStatus.AUTH_OK ? "authenticated"
						: "not authenticated")
				.kv("connection", open ? "open" : "closed")
				.kv("address", address)
				.kv("protocol", protocol)
				.line(MM.muted("Tip: ").append(MM.cmd("/cb ping"))
						.append(MM.muted(" to measure latency.")))
				.send(sender);
	}
}
