package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.out.ctx.PingRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.util.BarBuilder;
import dev.objz.commandbridge.util.MM;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PingCommand {

	private final SessionHub sessions;
	private final OutNode<Object> outNode;
	private final Duration pingTimeout;

	public PingCommand(SessionHub sessions, OutNode<Object> outNode, VelocityConfig config) {
		this.sessions = sessions;
		this.outNode = outNode;
		this.pingTimeout = Duration.ofSeconds(config.timeouts().pingTimeout());
	}

	public void execute(CommandSource sender, String clientId) {
		if (clientId == null) {
			pingAll(sender);
		} else {
			pingSingle(sender, clientId);
		}
	}

	private void pingAll(CommandSource sender) {
		List<ClientSession> activeClients = getActiveClients();

		if (activeClients.isEmpty()) {
			MM.msg().space().line(MM.warn("No authenticated clients to ping")).send(sender);
			return;
		}

		MM.msg().space().line(MM.accent("Pinging " + activeClients.size() + " client(s)...")).send(sender);

		AtomicInteger completed = new AtomicInteger(0);
		ConcurrentHashMap<String, PingResult> results = new ConcurrentHashMap<>();
		int total = activeClients.size();

		for (ClientSession session : activeClients) {
			String id = session.id();
			String address = session.ch() != null && session.ch().getSourceAddress() != null
					? session.ch().getSourceAddress().toString()
					: "unknown";

			try {
				outNode.send(
						MessageType.PING,
						new PingRequestContext(session, pingTimeout,
								(success, latency) -> {
									results.put(id, new PingResult(
											success ? latency : -1L,
											address));
									if (completed.incrementAndGet() == total) {
										displayResults(sender, results, total);
									}
								}));
			} catch (Exception ex) {
				results.put(id, new PingResult(-1L, address));
				if (completed.incrementAndGet() == total) {
					displayResults(sender, results, total);
				}
			}
		}
	}

	private void pingSingle(CommandSource sender, String clientId) {
		ClientSession session = findClient(clientId);

		if (session == null) {
			MM.msg().space().line(MM.error("Client '" + clientId + "' not found")).send(sender);
			return;
		}

		if (session.status() != AuthStatus.AUTH_OK) {
			MM.msg().space().line(MM.error("Client '" + clientId + "' is not authenticated")).send(sender);
			return;
		}

		if (session.ch() == null || !session.ch().isOpen()) {
			MM.msg().space().line(MM.error("Client '" + clientId + "' is not connected")).send(sender);
			return;
		}

		String ipAddress = session.ch().getSourceAddress().toString();

		try {
			outNode.send(
					MessageType.PING,
					new PingRequestContext(session, pingTimeout, (success, latency) -> {
						if (success && latency >= 0) {
							String badge = getLatencyBadge(latency);

							MM.msg()
									.space()
									.header("Ping Result")
									.item(badge + " <white>" + clientId
											+ "</white> <gray>" + ipAddress
											+ "</gray> "
											+ formatLatency(latency))
									.send(sender);
						} else {
							MM.msg()
									.space()
									.line(MM.error("Failed to ping '" + clientId
											+ "'"))
									.line(MM.muted("  Timeout or connection error"))
									.send(sender);
						}
					}));
		} catch (Exception ex) {
			MM.msg()
					.space()
					.line(MM.error("Failed to ping '" + clientId + "'"))
					.line(MM.muted("  " + ex.getMessage()))
					.send(sender);
		}
	}

	private List<ClientSession> getActiveClients() {
		List<ClientSession> activeClients = new ArrayList<>();
		for (ClientSession session : sessions) {
			if (session.status() == AuthStatus.AUTH_OK && session.ch() != null && session.ch().isOpen()) {
				activeClients.add(session);
			}
		}
		return activeClients;
	}

	private ClientSession findClient(String clientId) {
		for (ClientSession session : sessions) {
			if (session.id() != null && session.id().equalsIgnoreCase(clientId)) {
				return session;
			}
		}
		return null;
	}

	private void displayResults(CommandSource sender, ConcurrentHashMap<String, PingResult> results, int total) {
		int successful = 0;
		int highLatency = 0;
		int failed = 0;
		long minLatency = Long.MAX_VALUE;
		long maxLatency = 0;
		long totalLatency = 0;

		List<PingEntry> entries = new ArrayList<>();

		for (var entry : results.entrySet()) {
			PingResult result = entry.getValue();
			long latency = result.latency;
			entries.add(new PingEntry(entry.getKey(), latency, result.address));

			if (latency >= 0) {
				successful++;
				totalLatency += latency;
				minLatency = Math.min(minLatency, latency);
				maxLatency = Math.max(maxLatency, latency);
				if (latency >= 150) {
					highLatency++;
				}
			} else {
				failed++;
			}
		}

		entries.sort((a, b) -> Long.compare(a.latency, b.latency));

		String bar = BarBuilder.create(110)
				.add("green", (successful - highLatency) / (double) total)
				.add("yellow", highLatency / (double) total)
				.add("red", failed / (double) total)
				.build();

		var msg = MM.msg()
				.space()
				.header("Ping Results")
				.line(MM.parse(bar))
				.line(MM.kv("replied", "<green>" + successful + "</green>")
						.append(MM.sep())
						.append(MM.kv("high latency", "<yellow>" + highLatency + "</yellow>"))
						.append(MM.sep())
						.append(MM.kv("no reply", "<red>" + failed + "</red>")))
				.space()
				.line(MM.accent("Clients"));

		for (PingEntry entry : entries) {
			if (entry.latency >= 0) {
				msg.item(getLatencyBadge(entry.latency) + " <white>" + entry.id + "</white> <gray>"
						+ entry.address + "</gray> " + formatLatency(entry.latency));
			} else {
				msg.item("<red>[FAILED]</red> <white>" + entry.id + "</white> <gray>"
						+ entry.address + "</gray>");
			}
		}

		if (successful > 0) {
			long avgLatency = totalLatency / successful;
			msg.space()
					.line(MM.kv("min", formatLatency(minLatency))
							.append(MM.sep())
							.append(MM.kv("avg", formatLatency(avgLatency)))
							.append(MM.sep())
							.append(MM.kv("max", formatLatency(maxLatency))));
		}

		msg.send(sender);
	}

	private String getLatencyBadge(long ms) {
		if (ms < 30)
			return "<green>[EXCELLENT]</green>";
		if (ms < 75)
			return "<green>[GOOD]</green>";
		if (ms < 150)
			return "<yellow>[FAIR]</yellow>";
		return "<red>[POOR]</red>";
	}

	private String formatLatency(long ms) {
		if (ms < 30 || ms < 75)
			return "<green>" + ms + "ms</green>";
		if (ms < 150)
			return "<yellow>" + ms + "ms</yellow>";
		return "<red>" + ms + "ms</red>";
	}

	private static class PingEntry {
		final String id;
		final long latency;
		final String address;

		PingEntry(String id, long latency, String address) {
			this.id = id;
			this.latency = latency;
			this.address = address;
		}
	}

	private static class PingResult {
		final long latency;
		final String address;

		PingResult(long latency, String address) {
			this.latency = latency;
			this.address = address;
		}
	}
}
