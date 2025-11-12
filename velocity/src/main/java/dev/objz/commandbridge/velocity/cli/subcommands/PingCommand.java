package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.net.WsServer;
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
	@SuppressWarnings("unused")
	private final WsServer ws;

	public PingCommand(SessionHub sessions, WsServer ws) {
		this.sessions = sessions;
		this.ws = ws;
	}

	public void execute(CommandSource sender) {
		List<ClientSession> list = new ArrayList<>();
		for (ClientSession s : sessions)
			list.add(s);

		if (list.isEmpty()) {
			MM.msg().space().line(MM.warn("No clients to ping")).send(sender);
			return;
		}

		MM.msg().space().line(MM.accent("Pinging " + list.size() + " client(s)...")).send(sender);

		AtomicInteger done = new AtomicInteger(0);
		ConcurrentHashMap<String, Long> times = new ConcurrentHashMap<>();
		int total = list.size();

		for (ClientSession s : list) {
			String id = s.id();
			long start = System.nanoTime();

			try {
				var ch = s.ch();
				if (ch != null && ch.isOpen()) {
					WebSockets.sendPing(ByteBuffer.allocate(0), ch, new WebSocketCallback<>() {
						@Override
						public void complete(WebSocketChannel channel, Void context) {
							long ms = (System.nanoTime() - start) / 1_000_000;
							times.put(id, ms);
							if (done.incrementAndGet() == total)
								pushResults(sender, times, total);
						}

						@Override
						public void onError(WebSocketChannel channel, Void context,
								Throwable throwable) {
							times.put(id, -1L);
							if (done.incrementAndGet() == total)
								pushResults(sender, times, total);
						}
					});
				} else {
					times.put(id, -1L);
					if (done.incrementAndGet() == total)
						pushResults(sender, times, total);
				}
			} catch (Exception ex) {
				times.put(id, -1L);
				if (done.incrementAndGet() == total)
					pushResults(sender, times, total);
			}
		}

		if (done.get() == total)
			pushResults(sender, times, total);
	}

	private void pushResults(CommandSource sender, ConcurrentHashMap<String, Long> times, int total) {
		int excellent = 0;
		int good = 0;
		int poor = 0;
		int failed = 0;

		long minLatency = Long.MAX_VALUE;
		long maxLatency = 0;
		long totalLatency = 0;
		int successCount = 0;

		List<PingResult> results = new ArrayList<>();
		for (var e : times.entrySet()) {
			String id = e.getKey();
			long ms = e.getValue();

			if (ms >= 0) {
				successCount++;
				totalLatency += ms;
				minLatency = Math.min(minLatency, ms);
				maxLatency = Math.max(maxLatency, ms);

				if (ms < 50) {
					excellent++;
				} else if (ms < 100) {
					good++;
				} else {
					poor++;
				}
			} else {
				failed++;
			}

			results.add(new PingResult(id, ms));
		}

		results.sort((a, b) -> Long.compare(a.latency, b.latency));

		double greenPct = excellent / (double) total;
		double yellowPct = (good + poor) / (double) total;
		double redPct = failed / (double) total;

		String bar = BarBuilder.create(110)
				.add("green", greenPct)
				.add("yellow", yellowPct)
				.add("red", redPct)
				.build();

		var m = MM.msg()
				.space()
				.header("Ping Results")
				.line(MM.parse(bar));

		double successPct = total == 0 ? 0 : (successCount * 100.0 / total);
		long avgLatency = successCount > 0 ? (totalLatency / successCount) : 0;

		m.line(MM.kv("success rate", String.format("<green>%.1f%%</green> (%d/%d)", successPct, successCount, total))
						.append(MM.sep())
						.append(MM.kv("avg", formatLatency(avgLatency))))
				.space()
				.line(MM.accent("Breakdown"));

		for (PingResult result : results) {
			if (result.latency >= 0) {
				String badge;
				String color;
				if (result.latency < 50) {
					badge = "<green>[EXCELLENT]</green>";
					color = "<green>";
				} else if (result.latency < 100) {
					badge = "<yellow>[GOOD]</yellow>";
					color = "<yellow>";
				} else {
					badge = "<red>[POOR]</red>";
					color = "<red>";
				}
				m.item(badge + " <white>" + result.id + "</white> " + color + result.latency + "ms</" + color + ">");
			} else {
				m.item("<red>[FAILED]</red> <white>" + result.id + "</white> <gray>no response</gray>");
			}
		}

		if (successCount > 1) {
			m.space()
					.line(MM.accent("Statistics"))
					.kv("  min latency", formatLatency(minLatency))
					.kv("  avg latency", formatLatency(avgLatency))
					.kv("  max latency", formatLatency(maxLatency));
		}

		m.send(sender);
	}

	private String formatLatency(long ms) {
		if (ms < 50) {
			return "<green>" + ms + "ms</green>";
		} else if (ms < 100) {
			return "<yellow>" + ms + "ms</yellow>";
		} else {
			return "<red>" + ms + "ms</red>";
		}
	}

	private static class PingResult {
		final String id;
		final long latency;

		PingResult(String id, long latency) {
			this.id = id;
			this.latency = latency;
		}
	}
}
