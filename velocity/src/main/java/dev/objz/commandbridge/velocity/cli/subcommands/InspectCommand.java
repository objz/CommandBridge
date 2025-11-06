package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.MM;

public final class InspectCommand {
	private final SessionHub sessions;
	public InspectCommand(SessionHub sessions) { this.sessions = sessions; }

	public void execute(CommandSource sender, String id) {
		ClientSession target = null;
		for (ClientSession s : sessions) {
			if (s.id() != null && s.id().equalsIgnoreCase(id)) {
				target = s; break;
			}
		}
		if (target == null) {
			MM.msg().line(MM.error("Client '" + id + "' not found")).send(sender);
			return;
		}

		var ch = target.ch();
		boolean open = ch != null && ch.isOpen();
		String address = (ch != null && ch.getSourceAddress() != null) ? ch.getSourceAddress().toString() : "unknown";
		String protocol = (ch != null && ch.getVersion() != null) ? ch.getVersion().toHttpHeaderValue() : "unknown";

		MM.msg()
			.header("Client")
			.kv("id", target.id())
			.kv("status", target.status() == AuthStatus.AUTH_OK ? "authenticated" : "not authenticated")
			.kv("connection", open ? "open" : "closed")
			.kv("address", address)
			.kv("protocol", protocol)
			.line(MM.muted("Tip: ")
					.append(MM.cmd("/cb ping"))
					.append(MM.muted(" to measure latency.")))
			.send(sender);
	}
}
