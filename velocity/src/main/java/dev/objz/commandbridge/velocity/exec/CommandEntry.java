package dev.objz.commandbridge.velocity.exec;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.enums.RunAs;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.exec.stage.*;
import dev.objz.commandbridge.velocity.net.out.ctx.ExecuteCommandContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.CooldownManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class CommandEntry {

	private final ProxyServer proxy;
	private final ScriptManager scriptManager;
	private final SessionHub sessions;
	private final OutNode<Object> outNode;
	private final SchedulerManager scheduler;
	private final CooldownManager cooldowns;

	public CommandEntry(ProxyServer proxy, Object plugin, ScriptManager scriptManager,
			SessionHub sessions, OutNode<Object> outNode) {
		this.proxy = proxy;
		this.scriptManager = scriptManager;
		this.sessions = sessions;
		this.outNode = outNode;
		this.scheduler = new SchedulerManager(proxy, plugin);
		this.cooldowns = new CooldownManager();
	}

	/**
	 * Execute a command from an inbound InvokedCommand message (from backend).
	 */
	public void execute(InvokedCommand invoked, ClientSession originSession) {
		CommandSource source = resolveSource(invoked.sender());
		if (source == null) {
			Log.debug("Source resolution failed for invoked command '{}'", invoked.name());
			return;
		}

		ExecutionContext initial = new ExecutionContext(
				invoked,
				originSession,
				source,
				null,
				Map.of(),
				null,
				-1);

		runPipeline(initial);
	}

	/**
	 * Execute a command directly from Velocity command registration.
	 * This converts CommandAPI arguments to InvokedCommand format and runs the same
	 * pipeline.
	 */
	public void executeFromVelocity(String commandName, CommandSource source, CommandArguments args,
			CommandStub stub) {
		Log.debug("Executing Velocity command '{}' from source {}", commandName, source);

		SenderContext senderCtx = buildSenderContext(source);
		List<InvokedCommand.TypedArgument> typedArgs = buildTypedArguments(stub, args);

		InvokedCommand invoked = new InvokedCommand(commandName, typedArgs, senderCtx);

		ExecutionContext initial = new ExecutionContext(
				invoked,
				null, // No origin session for Velocity-initiated commands
				source,
				null,
				Map.of(),
				null,
				-1);

		runPipeline(initial);
	}

	private void runPipeline(ExecutionContext initial) {
		runStage(new ScriptResolutionStage(scriptManager), initial,
				ctx1 -> runStage(new ArgumentMappingStage(), ctx1,
						ctx2 -> runStage(new PermissionCheckStage(), ctx2,
								ctx3 -> runStage(new CooldownStage(cooldowns), ctx3,
										this::startCommandChain))));
	}

	private SenderContext buildSenderContext(CommandSource source) {
		if (source instanceof Player p) {
			return new SenderContext.Player(p.getUsername(), p.getUniqueId().toString());
		}
		return new SenderContext.Console();
	}

	/**
	 * Build typed arguments from all arguments defined in the stub.
	 * All arguments are included regardless of whether they are used in command
	 * placeholders.
	 */
	private List<InvokedCommand.TypedArgument> buildTypedArguments(CommandStub stub, CommandArguments args) {
		List<InvokedCommand.TypedArgument> typedArgs = new ArrayList<>();

		if (stub.args() == null || stub.args().isEmpty()) {
			return typedArgs;
		}

		for (ArgMapping mapping : stub.args()) {
			String name = mapping.name();
			ArgType type = mapping.type();
			Object raw = args.getOptional(name).orElse(null);

			Object value = convertArgumentValue(type, raw);
			typedArgs.add(new InvokedCommand.TypedArgument(type, value));
		}

		return typedArgs;
	}

	private Object convertArgumentValue(ArgType type, Object raw) {
		if (raw == null) {
			return null;
		}

		return switch (type) {
			case STRING, TEXT -> raw.toString();
			case INTEGER, TIME ->
				(raw instanceof Number n) ? n.intValue() : Integer.parseInt(raw.toString());
			case DOUBLE -> (raw instanceof Number n) ? n.doubleValue() : Double.parseDouble(raw.toString());
			case BOOLEAN -> (raw instanceof Boolean b) ? b : Boolean.parseBoolean(raw.toString());
			case SERVER -> raw.toString();
			default -> raw.toString();
		};
	}

	private void runStage(Pipeline stage, ExecutionContext ctx, Consumer<ExecutionContext> onSuccess) {
		stage.process(ctx, result -> {
			if (result instanceof ExecutionResult.Continue c) {
				onSuccess.accept(c.context());
			} else if (result instanceof ExecutionResult.Stop s) {
				Log.debug("Execution stopped:  {}", s.reason());
			} else if (result instanceof ExecutionResult.Error e) {
				Log.warn("Execution error:  {}", e.message());
			}
		});
	}

	private void startCommandChain(ExecutionContext ctx) {
		if (ctx.script() == null || ctx.script().commands() == null)
			return;
		processCommand(ctx, 0);
	}

	private void processCommand(ExecutionContext ctx, int index) {
		List<CmdMapping> commands = ctx.script().commands();
		if (index >= commands.size())
			return;

		CmdMapping cmd = commands.get(index);
		ExecutionContext nextCtx = ctx.nextCommand(cmd, index);

		runStage(new PlaceholderStage(), nextCtx, resolvedCtx -> {
			CmdMapping resolvedCmd = resolvedCtx.currentCommand();

			Runnable executionTask = () -> {
				dispatch(resolvedCtx, resolvedCmd);
				processCommand(resolvedCtx, index + 1);
			};

			Duration delay = resolvedCmd.delay();
			if (delay != null && !delay.isZero() && !delay.isNegative()) {
				scheduler.schedule(executionTask, delay);
			} else {
				executionTask.run();
			}
		});
	}

	private void dispatch(ExecutionContext ctx, CmdMapping cmd) {
		if (cmd.execute() == null || cmd.execute().isEmpty()) {
			Log.warn("Command '{}' has no execution targets", cmd.command());
			return;
		}

		for (IdMapping target : cmd.execute()) {
			if (target.location() == Location.VELOCITY) {
				dispatchVelocity(ctx, cmd);
			} else if (target.location() == Location.BACKEND) {
				dispatchBackend(ctx, cmd, target.id());
			}
		}
	}

	private void dispatchVelocity(ExecutionContext ctx, CmdMapping cmd) {
		CommandSource executor = ctx.source();

		if (cmd.runAs() != null) {
			switch (cmd.runAs()) {
				case CONSOLE:
					executor = proxy.getConsoleCommandSource();
					break;
				case PLAYER:
					if (!(executor instanceof Player)) {
						Log.warn("Cannot run as PLAYER when source is not a player");
						return;
					}
					break;
				case OPERATOR:
					// Treat as player or whatever source is
					break;
			}
		}

		String commandLine = cmd.command();
		if (commandLine.startsWith("/")) {
			commandLine = commandLine.substring(1);
		}

		Log.debug("Dispatching Velocity command:   '{}'", commandLine);
		proxy.getCommandManager().executeAsync(executor, commandLine);
	}

	private void dispatchBackend(ExecutionContext ctx, CmdMapping cmd, String targetClientId) {
		ClientSession targetSession = findSession(targetClientId);
		if (targetSession == null) {
			Log.warn("Target client '{}' not found for command execution", targetClientId);
			return;
		}

		if (targetSession.ch() == null || !targetSession.ch().isOpen()) {
			Log.warn("Target client '{}' is not connected", targetClientId);
			return;
		}

		UUID uuid = (ctx.source() instanceof Player p) ? p.getUniqueId() : null;
		RunAs runAs = cmd.runAs() != null ? cmd.runAs() : RunAs.CONSOLE;

		Log.debug("Dispatching backend command to '{}':   command='{}', runAs={}, uuid={}",
				targetClientId, cmd.command(), runAs, uuid);

		ExecuteCommandContext payload = new ExecuteCommandContext(
				targetSession,
				cmd.command(),
				runAs,
				uuid);

		outNode.send(MessageType.EXECUTE_COMMAND, payload);
	}

	private CommandSource resolveSource(SenderContext sender) {
		if (sender instanceof SenderContext.Player p) {
			try {
				UUID uid = UUID.fromString(p.uuid());
				return proxy.getPlayer(uid).map(player -> (CommandSource) player).orElse(null);
			} catch (Exception e) {
				return null;
			}
		}
		return proxy.getConsoleCommandSource();
	}

	private ClientSession findSession(String id) {
		for (ClientSession s : sessions) {
			if (id.equals(s.id()))
				return s;
		}
		return null;
	}
}
