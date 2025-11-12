package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.MM;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
			MM.msg().space().line(MM.error("Client '" + id + "' not found")).send(sender);
			return;
		}

		boolean authenticated = target.status() == AuthStatus.AUTH_OK;
		boolean open = target.ch() != null && target.ch().isOpen();

		String statusBadge;
		if (authenticated && open) {
			statusBadge = "<green>authenticated & connected</green>";
		} else if (open) {
			statusBadge = "<yellow>connected (not authenticated)</yellow>";
		} else {
			statusBadge = "<red>disconnected</red>";
		}

		String address = target.ch() != null && target.ch().getSourceAddress() != null
				? target.ch().getSourceAddress().toString()
				: "unknown";

		String protocol = "unknown";
		if (target.ch() != null) {
			String subProtocol = target.ch().getSubProtocol();
			protocol = subProtocol != null ? "WebSocket (" + subProtocol + ")" : "WebSocket";
		}

		String channelState = "unknown";
		if (target.ch() != null) {
			channelState = target.ch().isOpen() ? "<green>open</green>" : "<red>closed</red>";
		}

		long uptime = target.connectedAt() != null
				? Duration.between(target.connectedAt(), Instant.now()).toSeconds()
				: 0;
		String uptimeStr = formatUptime(uptime);

		var m = MM.msg()
				.space()
				.header("Client Inspection")
				.space()
				.line(MM.accent("Identity"))
				.kv("  id", target.id() != null ? target.id() : "unknown")
				.kv("  status", statusBadge)
				.space()
				.line(MM.accent("Connection"))
				.kv("  state", channelState)
				.kv("  address", address)
				.kv("  protocol", protocol);

		if (uptime > 0) {
			m.kv("  uptime", uptimeStr);
		}

		if (target.lastActivity() != null) {
			long secondsAgo = Duration.between(target.lastActivity(), Instant.now()).toSeconds();
			m.kv("  last activity", secondsAgo + "s ago");
		}

		m.space()
				.line(MM.muted("Tip: Use ").append(MM.cmd("/cb ping"))
						.append(MM.muted(" to measure latency")))
				.send(sender);
	}

	private String formatUptime(long seconds) {
		long days = TimeUnit.SECONDS.toDays(seconds);
		long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
		long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
		long secs = seconds % 60;

		if (days > 0) {
			return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
		} else if (hours > 0) {
			return String.format("%dh %dm %ds", hours, minutes, secs);
		} else if (minutes > 0) {
			return String.format("%dm %ds", minutes, secs);
		} else {
			return secs + "s";
		}
	}
}
