package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.RegistrationManager;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.net.out.ctx.RegistrationRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.util.BarBuilder;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.util.MM.MessageBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;

public final class ReloadCommand {

	private final ConfigManager configManager;
	private final ScriptManager scriptManager;
	private final RegistrationManager registrationManager;
	private final SessionHub sessionHub;
	private final OutNode<Object> outNode;

	public ReloadCommand(ConfigManager configManager, ScriptManager scriptManager,
			RegistrationManager registrationManager, SessionHub sessionHub,
			OutNode<Object> outNode) {
		this.configManager = configManager;
		this.scriptManager = scriptManager;
		this.registrationManager = registrationManager;
		this.sessionHub = sessionHub;
		this.outNode = outNode;
	}

	public void execute(CommandSource sender) {
		try {
			boolean configOk = configManager.reload(VelocityConfig.class);
			var cfg = configManager.current(VelocityConfig.class);
			if (!configOk || cfg == null) {
				MM.msg().space().line(MM.error("Failed to reload config"))
						.line(MM.muted("Check console for details")).send(sender);
				return;
			}
			Log.setDebug(cfg.debug());

			scriptManager.loadAll();

			int enabled = scriptManager.enabled().size();
			int disabled = scriptManager.disabled().size();
			int loaded = scriptManager.loaded().size();
			int errors = (int) scriptManager.errors();

			double green = enabled / (double) Math.max(loaded, 1);
			double yellow = (disabled - errors) / (double) Math.max(loaded, 1);
			double red = Math.min(errors, loaded) / (double) Math.max(loaded, 1);

			String scriptBar = BarBuilder.create(110).add("green", green).add("yellow", yellow)
					.add("red", red).build();

			var summary = MM.msg().space().line(MM.parse(scriptBar))
					.line(MM.kv("loaded", String.valueOf(loaded)).append(MM.sep())
							.append(MM.kv("enabled", "<green>" + enabled + "</green>"))
							.append(MM.sep())
							.append(MM.kv("disabled", "<yellow>" + disabled + "</yellow>"))
							.append(MM.sep())
							.append(MM.kv("errors", "<red>" + errors + "</red>")));

			if (errors > 0) {
				List<Component> summaryLines = summary.getLines();
				MM.msg().space()
						.line(MM.error("Script loading failed with " + errors + " error(s)"))
						.line(summaryLines.get(1)).line(summaryLines.get(2)).space()
						.line(MM.warn("Reload aborted due to script errors"))
						.line(MM.muted("Check console for details")).send(sender);
				return;
			}

			List<Component> summaryLines = summary.getLines();
			MessageBuilder resultMessage = MM.msg().space().line(MM.ok("Config and scripts reloaded"))
					.line(summaryLines.get(1)).line(summaryLines.get(2));

			registrationManager.load(scriptManager.enabled());

			List<ClientSession> activeClients = getActiveClients();
			int clientsWithScripts = (int) activeClients.stream().filter(
					s -> !registrationManager.getScriptsForSession(s).isEmpty())
					.count();

			if (clientsWithScripts == 0) {
				resultMessage.send(sender);
				return;
			}

			resultMessage.space().line(MM.accent(
					"Re-registering commands for " + clientsWithScripts + " client(s)..."));

			Duration registerTimeout = Duration.ofSeconds(cfg.timeouts().registerTimeout());
			AtomicInteger completed = new AtomicInteger(0);
			ConcurrentHashMap<String, ReloadResult> results = new ConcurrentHashMap<>();
			final int totalExpected = clientsWithScripts;
			final var sentTo = new ConcurrentHashMap<String, Boolean>();

			for (ClientSession session : activeClients) {
				String clientId = session.id();
				String address = session.ch() != null && session.ch().getSourceAddress() != null
						? session.ch().getSourceAddress().toString()
						: "unknown";

				Set<dev.objz.commandbridge.scripting.model.Script> scripts = registrationManager
						.getScriptsForSession(session);

				if (scripts == null || scripts.isEmpty()) {
					continue;
				}

				sentTo.put(clientId, true);
				try {
					outNode.send(MessageType.REGISTER_COMMANDS,
							new RegistrationRequestContext(session, scripts,
									registerTimeout,
									(success) -> {
										ReloadStatus status = success
												? ReloadStatus.SUCCESS
												: ReloadStatus.FAILED;
										results.put(clientId,
												new ReloadResult(status,
														address,
														null));
										if (completed.incrementAndGet() == totalExpected) {
											displayResults(sender, results,
													totalExpected,
													resultMessage);
										}
									}));
				} catch (Exception ex) {
					results.put(clientId, new ReloadResult(ReloadStatus.FAILED, address,
							ex.getMessage()));
					if (completed.incrementAndGet() == totalExpected) {
						displayResults(sender, results, totalExpected, resultMessage);
					}
				}
			}

			if (totalExpected > 0) {
				new Thread(() -> {
					try {
						Thread.sleep(registerTimeout.toMillis() + 1000);
						if (completed.get() < totalExpected) {
							for (ClientSession session : activeClients) {
								String clientId = session.id();
								if (sentTo.containsKey(clientId)
										&& !results.containsKey(clientId)) {
									String address = session.ch() != null && session
											.ch()
											.getSourceAddress() != null
													? session.ch().getSourceAddress()
															.toString()
													: "unknown";
									results.put(clientId,
											new ReloadResult(
													ReloadStatus.TIMEOUT,
													address,
													"Registration timeout after "
															+ registerTimeout
																	.toSeconds()
															+ "s"));
								}
							}
							if (completed.get() < totalExpected) {
								completed.set(totalExpected);
								displayResults(sender, results, totalExpected,
										resultMessage);
							}
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}).start();
			} else {
				resultMessage.send(sender);
			}
		} catch (Exception e) {
			Log.error("Reload failed: {}", e.getMessage());
			MM.msg().space().line(MM.error("Reload failed: " + e.getMessage()))
					.line(MM.muted("Check console for details")).send(sender);
		}
	}

	private List<ClientSession> getActiveClients() {
		List<ClientSession> activeClients = new ArrayList<>();
		for (ClientSession session : sessionHub) {
			if (session.status() == AuthStatus.AUTH_OK && session.ch() != null
					&& session.ch().isOpen()) {
				activeClients.add(session);
			}
		}
		return activeClients;
	}

	private void displayResults(CommandSource sender,
			ConcurrentHashMap<String, ReloadResult> results, int total,
			MessageBuilder resultMessage) {
		int successful = 0;
		int failed = 0;
		int timeout = 0;

		List<ReloadEntry> entries = new ArrayList<>();
		for (var entry : results.entrySet()) {
			ReloadResult result = entry.getValue();
			entries.add(new ReloadEntry(entry.getKey(), result.status, result.address,
					result.errorMessage));

			switch (result.status) {
				case SUCCESS -> successful++;
				case FAILED -> failed++;
				case TIMEOUT -> timeout++;
			}
		}

		entries.sort((a, b) -> {
			if (a.status != b.status) {
				return a.status.ordinal() - b.status.ordinal();
			}
			return a.id.compareTo(b.id);
		});

		String clientBar = BarBuilder.create(110)
				.add("green", successful / (double) Math.max(1, total))
				.add("red", failed / (double) Math.max(1, total))
				.add("yellow", timeout / (double) Math.max(1, total)).build();

		resultMessage.space().header("Client Registration Results").line(MM.parse(clientBar))
				.line(MM.kv("successful", "<green>" + successful + "</green>").append(MM.sep())
						.append(MM.kv("failed", "<red>" + failed + "</red>")).append(MM.sep())
						.append(MM.kv("timeout", "<yellow>" + timeout + "</yellow>")))
				.space().line(MM.accent("Clients"));

		for (ReloadEntry entry : entries) {
			switch (entry.status) {
				case SUCCESS -> resultMessage.item(
						"<green>[OK]</green> <white>" + entry.id + "</white> <gray>"
								+ entry.address + "</gray>");
				case FAILED -> {
					String errorDetail = entry.errorMessage != null
							? " <gray>(" + entry.errorMessage + ")</gray>"
							: "";
					resultMessage.item("<red>[FAILED]</red> <white>" + entry.id + "</white> <gray>"
							+ entry.address + "</gray>" + errorDetail);
				}
				case TIMEOUT -> {
					String errorDetail = entry.errorMessage != null
							? " <gray>(" + entry.errorMessage + ")</gray>"
							: "";
					resultMessage.item("<yellow>[TIMEOUT]</yellow> <white>" + entry.id
							+ "</white> <gray>" + entry.address + "</gray>" + errorDetail);
				}
			}
		}

		if (failed > 0 || timeout > 0) {
			resultMessage.space().line(MM.muted("Check console for detailed error messages"));
		}

		resultMessage.send(sender);
	}

	private enum ReloadStatus {
		SUCCESS, FAILED, TIMEOUT
	}

	private static class ReloadEntry {
		final String id;
		final ReloadStatus status;
		final String address;
		final String errorMessage;

		ReloadEntry(String id, ReloadStatus status, String address, String errorMessage) {
			this.id = id;
			this.status = status;
			this.address = address;
			this.errorMessage = errorMessage;
		}
	}

	private static class ReloadResult {
		final ReloadStatus status;
		final String address;
		final String errorMessage;

		ReloadResult(ReloadStatus status, String address, String errorMessage) {
			this.status = status;
			this.address = address;
			this.errorMessage = errorMessage;
		}
	}
}
