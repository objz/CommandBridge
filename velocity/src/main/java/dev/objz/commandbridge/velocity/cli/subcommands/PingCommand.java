package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
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
			MM.msg().line(MM.warn("No clients to ping")).send(sender);
			return;
		}

		MM.msg().line(MM.accent("Pinging " + list.size() + " client(s)...")).send(sender);

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
		var m = MM.msg();

		int ok = 0;
		for (var e : times.entrySet()) {
			String id = e.getKey();
			long ms = e.getValue();
			if (ms >= 0) {
				ok++;
				String color = (ms < 50) ? "<green>" : (ms < 150) ? "<yellow>" : "<red>";
				m.item(color + "✓</> <white>" + id + "</white> (" + ms + "ms)");
			} else {
				m.item("<red>✗</red> <white>" + id + "</white> (timeout)");
			}
		}

		double pct = total == 0 ? 0 : (ok * 100.0 / total);
		String bar = progress(pct);

		m.space()
				.kv("success", String.format("%.0f%% %s", pct, bar))
				.send(sender);
	}

	private String progress(double pct) {
		int filled = (int) Math.round(pct / 10.0);
		StringBuilder sb = new StringBuilder("<gray>[</gray>");
		for (int i = 0; i < 10; i++)
			sb.append(i < filled ? "<green>■</green>" : "<gray>□</gray>");
		sb.append("<gray>]</gray>");
		return sb.toString();
	}
}
