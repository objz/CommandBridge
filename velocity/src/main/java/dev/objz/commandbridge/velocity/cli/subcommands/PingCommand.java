package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.BarBuilder;
import dev.objz.commandbridge.velocity.util.MM;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PingCommand {

	private final SessionHub sessions;

	public PingCommand(SessionHub sessions) {
		this.sessions = sessions;
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
		ConcurrentHashMap<String, Long> results = new ConcurrentHashMap<>();
		int total = activeClients.size();

		for (ClientSession session : activeClients) {
			String id = session.id();
			long startTime = System.nanoTime();

			try {
				WebSockets.sendPing(ByteBuffer.allocate(0), session.ch(), new WebSocketCallback<>() {
					@Override
					public void complete(WebSocketChannel channel, Void context) {
						long latency = (System.nanoTime() - startTime) / 1_000_000;
						results.put(id, latency);
						if (completed.incrementAndGet() == total) {
							displayResults(sender, results, total);
						}
					}

					@Override
					public void onError(WebSocketChannel channel, Void context,
							Throwable throwable) {
						results.put(id, -1L);
						if (completed.incrementAndGet() == total) {
							displayResults(sender, results, total);
						}
					}
				});
			} catch (Exception ex) {
				results.put(id, -1L);
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
		long startTime = System.nanoTime();

		try {
			WebSockets.sendPing(ByteBuffer.allocate(0), session.ch(), new WebSocketCallback<>() {
				@Override
				public void complete(WebSocketChannel channel, Void context) {
					long latency = (System.nanoTime() - startTime) / 1_000_000;
					String badge = getLatencyBadge(latency);

					MM.msg()
							.space()
							.header("Ping Result")
							.item(badge + " <white>" + clientId + "</white> "
									+ formatLatency(latency))
							.line(MM.muted("  Address: " + ipAddress))
							.send(sender);
				}

				@Override
				public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
					MM.msg()
							.space()
							.line(MM.error("Failed to ping '" + clientId + "'"))
							.line(MM.muted("  " + throwable.getMessage()))
							.send(sender);
				}
			});
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

	private void displayResults(CommandSource sender, ConcurrentHashMap<String, Long> results, int total) {
		int successful = 0;
		int highLatency = 0;
		int failed = 0;
		long minLatency = Long.MAX_VALUE;
		long maxLatency = 0;
		long totalLatency = 0;

		List<PingEntry> entries = new ArrayList<>();

		for (var entry : results.entrySet()) {
			long latency = entry.getValue();
			entries.add(new PingEntry(entry.getKey(), latency));

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
				msg.item(getLatencyBadge(entry.latency) + " <white>" + entry.id + "</white> "
						+ formatLatency(entry.latency));
			} else {
				msg.item("<red>[NO REPLY]</red> <white>" + entry.id + "</white>");
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

		PingEntry(String id, long latency) {
			this.id = id;
			this.latency = latency;
		}
	}
}
