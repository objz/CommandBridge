package dev.objz.commandbridge.velocity.dispatch;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.net.payloads.feedback.Feedback;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.logging.Summary;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.dispatch.exec.VelocityExecutor;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import dev.objz.commandbridge.velocity.dispatch.model.Pipeline;
import dev.objz.commandbridge.velocity.dispatch.stage.*;
import dev.objz.commandbridge.velocity.net.out.ctx.ExecuteCommandContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.CooldownManager;
import dev.objz.commandbridge.velocity.util.MM;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public final class CommandEntry {

	private final ProxyServer proxy;
	private final SessionHub sessions;
	private final OutNode<Object> outNode;
	private final ScheduleManager scheduler;
	private final VelocityExecutor velocityExecutor;
	private final List<Pipeline> pipelineStages;

	public CommandEntry(
			ProxyServer proxy,
			Object plugin,
			ScriptManager scriptManager,
			SessionHub sessions,
			OutNode<Object> outNode,
			String localServerId) {

		this.proxy = Objects.requireNonNull(proxy);
		this.sessions = Objects.requireNonNull(sessions);
		this.outNode = Objects.requireNonNull(outNode);
		this.scheduler = new ScheduleManager(proxy, Objects.requireNonNull(plugin));
		this.velocityExecutor = new VelocityExecutor(proxy,
				Objects.requireNonNull(localServerId));

		var cooldowns = new CooldownManager();
		this.pipelineStages = List.of(
				new ScriptResolutionStage(scriptManager),
				new ArgumentMappingStage(),
				new PermissionCheckStage(),
				new CooldownStage(cooldowns));
	}

	public VelocityExecutor getVelocityExecutor() {
		return velocityExecutor;
	}

	// ──────────────────────────────────────────────────────────────────────────────

	public void execute(InvokedCommand invoked, ClientSession originSession) {
		resolveSource(invoked.sender())
				.ifPresentOrElse(
						source -> runPipeline(
								createInitialContext(invoked, originSession, source)),
						() -> Log.debug("Source resolution failed for invoked command '{}'",
								invoked.name()));
	}

	public void executeFromVelocity(String commandName, CommandSource source, CommandArguments args,
			CommandStub stub) {
		Log.debug("Executing Velocity command '{}' from source {}", commandName, source);

		var invoked = new InvokedCommand(
				commandName,
				buildTypedArguments(stub, args),
				buildSenderContext(source));

		runPipeline(createInitialContext(invoked, null, source));
	}

	// ──────────────────────────────────────────────────────────────────────────────

	private void runPipeline(ExecutionContext initial) {
		runPipelineStages(pipelineStages.iterator(), initial, this::executeCommands);
	}

	private void runPipelineStages(Iterator<Pipeline> stages, ExecutionContext ctx,
			Consumer<ExecutionContext> onComplete) {
		if (!stages.hasNext()) {
			onComplete.accept(ctx);
			return;
		}

		stages.next().process(ctx, result -> handlePipelineResult(result, stages, onComplete));
	}

	private void handlePipelineResult(ExecutionResult result, Iterator<Pipeline> stages,
			Consumer<ExecutionContext> onComplete) {
		switch (result) {
			case ExecutionResult.Continue(var ctx) -> runPipelineStages(stages, ctx, onComplete);
			case ExecutionResult.Stop(var reason) -> Log.debug("Execution stopped: {}", reason);
			case ExecutionResult.Error(var message) -> Log.warn("Execution error: {}", message);
		}
	}

	// ──────────────────────────────────────────────────────────────────────────────

	private void executeCommands(ExecutionContext ctx) {
		Optional.ofNullable(ctx.script())
				.map(Script::commands)
				.filter(Predicate.not(List::isEmpty))
				.ifPresent(commands -> processCommandAt(ctx, commands, 0));
	}

	private void processCommandAt(ExecutionContext ctx, List<CmdMapping> commands, int index) {
		if (index >= commands.size())
			return;

		var cmd = commands.get(index);
		var nextCtx = ctx.nextCommand(cmd, index);

		new PlaceholderStage().process(nextCtx, result -> {
			if (result instanceof ExecutionResult.Continue(var resolvedCtx)) {
				scheduleAndExecute(resolvedCtx, () -> processCommandAt(ctx, commands, index + 1));
			}
		});
	}

	private void scheduleAndExecute(ExecutionContext ctx, Runnable continuation) {
		var cmd = ctx.currentCommand();
		var delay = Optional.ofNullable(cmd.delay()).filter(this::isPositiveDuration);

		Runnable task = () -> {
			dispatchCommand(ctx, cmd);
			continuation.run();
		};

		delay.ifPresentOrElse(
				d -> scheduler.schedule(task, d),
				task);
	}

	// ──────────────────────────────────────────────────────────────────────────────

	private void dispatchCommand(ExecutionContext ctx, CmdMapping cmd) {
		var targets = Optional.ofNullable(cmd.execute()).orElse(List.of());

		if (targets.isEmpty()) {
			Log.warn("Command '{}' has no execution targets defined", cmd.command());
			notifyExecutionError(ctx.source(), cmd.command(), "No execution targets configured");
			return;
		}

		targets.forEach(target -> dispatchToTarget(ctx, cmd, target));
	}

	private void dispatchToTarget(ExecutionContext ctx, CmdMapping cmd, IdMapping target) {
		String targetId = target.id();
		Location targetLoc = target.location();

		if (targetLoc == Location.VELOCITY) {
			if (velocityExecutor.isLocal(targetId)) {
				executeLocally(ctx, cmd);
			} else {
				dispatchToRemoteSession(ctx, cmd, targetId, Location.VELOCITY);
			}
		} else if (targetLoc == Location.BACKEND) {
			dispatchToRemoteSession(ctx, cmd, targetId, Location.BACKEND);
		} else {
			Log.warn("Unknown location type '{}' for target '{}'", targetLoc, targetId);
			notifyExecutionError(ctx.source(), cmd.command(),
					"Unknown location type: " + targetLoc);
		}
	}

	private void executeLocally(ExecutionContext ctx, CmdMapping cmd) {
		var playerUuid = extractPlayerUuid(ctx.source());
		var runAs = Optional.ofNullable(cmd.runAs()).orElse(RunAs.CONSOLE);

		velocityExecutor.execute(cmd.command(), runAs, playerUuid, ctx.source())
				.thenAccept(success -> {
					if (!success) {
						Log.warn("Local Velocity command '{}' execution failed", cmd.command());
						notifyExecutionError(ctx.source(), cmd.command(),
								"Command execution failed");

						Feedback feedback = new Feedback(1, 0, 1, List.of(),
								List.of("Local command execution failed:  "
										+ cmd.command()));
						Summary.feedbackSummary("Velocity Execution", feedback, "local");
					}
				})
				.exceptionally(ex -> {
					Log.error(ex, "Local Velocity command '{}' threw exception", cmd.command());
					notifyExecutionError(ctx.source(), cmd.command(), ex.getMessage());

					Feedback feedback = new Feedback(1, 0, 1, List.of(),
							List.of("Exception:  " + ex.getMessage()));
					Summary.feedbackSummary("Velocity Execution", feedback, "local");
					Summary.feedbackDetails(feedback, "local", false);
					return null;
				});
	}

	private void dispatchToRemoteSession(ExecutionContext ctx, CmdMapping cmd, String targetId,
			Location requiredLocation) {
		findSession(targetId, requiredLocation)
				.filter(this::isSessionConnected)
				.ifPresentOrElse(
						session -> sendExecuteCommand(session, ctx, cmd, targetId),
						() -> {
							Log.warn("Target '{}' ({}) not found or not connected",
									targetId,
									requiredLocation);
							notifyExecutionError(ctx.source(), cmd.command(),
									requiredLocation + " server '" + targetId
											+ "' is not connected");

							Feedback feedback = new Feedback(1, 0, 1, List.of(),
									List.of(requiredLocation + " not connected: "
											+ targetId));
							Summary.feedbackSummary("Execution Failed", feedback, targetId);
						});
	}

	private void sendExecuteCommand(ClientSession session, ExecutionContext ctx, CmdMapping cmd, String targetId) {
		var uuid = extractPlayerUuid(ctx.source());
		var runAs = Optional.ofNullable(cmd.runAs()).orElse(RunAs.CONSOLE);

		Set<String> grantedPermissions = null;
		if (runAs == RunAs.OPERATOR && ctx.script() != null) {
			grantedPermissions = buildGrantedPermissions(ctx.script(), cmd);
		}

		Log.debug("Dispatching command to '{}': command='{}', runAs={}, uuid={}, permissions={}",
				targetId, cmd.command(), runAs, uuid,
				grantedPermissions != null ? grantedPermissions.size() : 0);

		outNode.send(MessageType.EXECUTE_COMMAND,
				new ExecuteCommandContext(session, cmd.command(), runAs, uuid, grantedPermissions));
	}

	private Set<String> buildGrantedPermissions(Script script, CmdMapping currentCmd) {
		Set<String> permissions = new HashSet<>();

		permissions.add("commandbridge.command." + script.name());

		if (script.commands() != null) {
			for (CmdMapping cmd : script.commands()) {
				if (cmd.command() != null && !cmd.command().isBlank()) {
					String baseCommand = extractBaseCommand(cmd.command());
					if (baseCommand != null) {
						permissions.add(baseCommand);
						permissions.add(baseCommand + ".*");
					}
				}
			}
		}

		String currentBase = extractBaseCommand(currentCmd.command());
		if (currentBase != null) {
			permissions.add(currentBase);
			permissions.add(currentBase + ".*");
		}

		return permissions;
	}

	private String extractBaseCommand(String command) {
		if (command == null || command.isBlank()) {
			return null;
		}
		String trimmed = command.trim();
		if (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1);
		}
		int spaceIndex = trimmed.indexOf(' ');
		return spaceIndex > 0 ? trimmed.substring(0, spaceIndex) : trimmed;
	}

	private void notifyExecutionError(CommandSource source, String command, String errorMessage) {
		if (source == null) {
			return;
		}

		source.sendMessage(MM.parse("<red>⚠</red> <gray>Command execution failed</gray>"));
		source.sendMessage(
				MM.parse("<dark_gray>If this persists, please contact an administrator</dark_gray>"));

		if (source.hasPermission("commandbridge.admin")) {
			source.sendMessage(MM.parse("<dark_gray>Command: </dark_gray><white>" + command + "</white>"));
			if (errorMessage != null && !errorMessage.isBlank()) {
				source.sendMessage(MM.parse(
						"<dark_gray>Error: </dark_gray><red>" + errorMessage + "</red>"));
			}
		}
	}

	// ──────────────────────────────────────────────────────────────────────────────

	private ExecutionContext createInitialContext(InvokedCommand invoked, ClientSession session,
			CommandSource source) {
		return new ExecutionContext(invoked, session, source, null, Map.of(), null, -1);
	}

	private SenderContext buildSenderContext(CommandSource source) {
		return switch (source) {
			case Player p -> new SenderContext.Player(p.getUsername(), p.getUniqueId().toString());
			default -> new SenderContext.Console();
		};
	}

	private List<InvokedCommand.TypedArgument> buildTypedArguments(CommandStub stub, CommandArguments args) {
		return Optional.ofNullable(stub.args())
				.orElse(List.of())
				.stream()
				.map(mapping -> createTypedArgument(mapping, args))
				.toList();
	}

	private InvokedCommand.TypedArgument createTypedArgument(ArgMapping mapping, CommandArguments args) {
		var value = args.getOptional(mapping.name())
				.map(raw -> convertArgumentValue(mapping.type(), raw))
				.orElse(null);
		return new InvokedCommand.TypedArgument(mapping.type(), value);
	}

	private Object convertArgumentValue(ArgType type, Object raw) {
		if (raw == null)
			return null;

		return switch (type) {
			case STRING, TEXT -> raw.toString();
			case INTEGER, TIME -> toInteger(raw);
			case DOUBLE -> toDouble(raw);
			case BOOLEAN -> toBoolean(raw);
			case SERVER -> raw.toString();
			default -> raw.toString();
		};
	}

	// ──────────────────────────────────────────────────────────────────────────────

	private Optional<CommandSource> resolveSource(SenderContext sender) {
		return switch (sender) {
			case SenderContext.Player p -> parseUuid(p.uuid()).flatMap(this::findPlayer);
			case SenderContext.Console() -> Optional.of(proxy.getConsoleCommandSource());
			default -> Optional.of(proxy.getConsoleCommandSource());
		};
	}

	private Optional<ClientSession> findSession(String id, Location requiredLocation) {
		return StreamSupport.stream(sessions.spliterator(), false)
				.filter(s -> id.equals(s.id()) && s.location() == requiredLocation)
				.findFirst();
	}

	private Optional<Player> findPlayer(UUID uuid) {
		return proxy.getPlayer(uuid);
	}

	// ──────────────────────────────────────────────────────────────────────────────

	private Optional<UUID> parseUuid(String uuid) {
		try {
			return Optional.of(UUID.fromString(uuid));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private UUID extractPlayerUuid(CommandSource source) {
		return source instanceof Player p ? p.getUniqueId() : null;
	}

	private boolean isSessionConnected(ClientSession session) {
		return session.ch() != null && session.ch().isOpen();
	}

	private boolean isPositiveDuration(Duration d) {
		return !d.isZero() && !d.isNegative();
	}

	private int toInteger(Object raw) {
		return raw instanceof Number n ? n.intValue() : Integer.parseInt(raw.toString());
	}

	private double toDouble(Object raw) {
		return raw instanceof Number n ? n.doubleValue() : Double.parseDouble(raw.toString());
	}

	private boolean toBoolean(Object raw) {
		return raw instanceof Boolean b ? b : Boolean.parseBoolean(raw.toString());
	}
}
