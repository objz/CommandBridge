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
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.dispatch.exec.VelocityExecutor;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import dev.objz.commandbridge.velocity.dispatch.model.Pipeline;
import dev.objz.commandbridge.velocity.dispatch.stage.*;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.CooldownManager;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CommandEntry {

	private final ProxyServer proxy;
	private final Object plugin;
	private final SessionHub sessions;
	private final ScheduleManager scheduler;
	private final VelocityExecutor velocityExecutor;
	private final CommandDispatcher dispatcher;
	private final List<Pipeline> pipelineStages;

	public CommandEntry(
			ProxyServer proxy,
			Object plugin,
			ScriptManager scriptManager,
			SessionHub sessions,
			OutNode<Object> outNode,
			String localServerId,
			Path dataDir) {

		this.proxy = Objects.requireNonNull(proxy);
		this.plugin = Objects.requireNonNull(plugin);
		this.sessions = Objects.requireNonNull(sessions);

		this.scheduler = new ScheduleManager(proxy, plugin, dataDir, scriptManager);
		this.scheduler.setExecutionCallback(this::resumeTask);

		this.velocityExecutor = new VelocityExecutor(proxy, Objects.requireNonNull(localServerId));

		this.dispatcher = new CommandDispatcher(sessions, outNode, velocityExecutor);

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

	private void runPipeline(ExecutionContext initial) {
		runPipelineStages(pipelineStages.iterator(), initial, this::executeCommands);
	}

	private void runPipelineStages(Iterator<Pipeline> stages, ExecutionContext ctx,
			Consumer<ExecutionContext> onComplete) {
		if (!stages.hasNext()) {
			onComplete.accept(ctx);
			return;
		}

		stages.next().process(ctx, result -> {
			if (result instanceof ExecutionResult.Continue c) {
				runPipelineStages(stages, c.context(), onComplete);
			} else if (result instanceof ExecutionResult.Stop s) {
				Log.debug("Execution stopped: {}", s.reason());
			} else if (result instanceof ExecutionResult.Error e) {
				Log.warn("Execution error: {}", e.message());
			}
		});
	}

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
			if (result instanceof ExecutionResult.Continue c) {
				scheduleAndExecute(c.context(), () -> processCommandAt(ctx, commands, index + 1));
			}
		});
	}

	private void resumeTask(ExecutionContext ctx) {
		CmdMapping cmd = ctx.currentCommand();
		if (cmd != null) {
			dispatcher.dispatchCommand(ctx, cmd);
		}

		if (ctx.script() != null) {
			processCommandAt(ctx, ctx.script().commands(), ctx.commandIndex() + 1);
		}
	}

	private void scheduleAndExecute(ExecutionContext ctx, Runnable continuation) {
		var cmd = ctx.currentCommand();

		if (shouldSchedule(cmd)) {
			scheduler.queueTask(ctx, cmd, ctx.commandIndex());
			return;
		}

		Runnable task = () -> {
			dispatcher.dispatchCommand(ctx, cmd);
			continuation.run();
		};

		if (cmd.delay() != null && !cmd.delay().isZero() && !cmd.delay().isNegative()) {
			long delayMillis = cmd.delay().toMillis();
			proxy.getScheduler().buildTask(plugin, task)
					.delay(delayMillis, TimeUnit.MILLISECONDS)
					.schedule();
		} else {
			task.run();
		}
	}

	private boolean shouldSchedule(CmdMapping cmd) {
		if (cmd.server() == null || !cmd.server().scheduleOnline()) {
			return false;
		}

		if (cmd.execute() == null || cmd.execute().isEmpty()) {
			return false;
		}

		for (IdMapping target : cmd.execute()) {
			if (target.location() == Location.BACKEND) {
				boolean online = sessions.findSession(target.id(), Location.BACKEND).isPresent();
				if (!online) {
					return true;
				}
			}
		}

		return false;
	}

	// ──────────────────────────────────────────────────────────────────────────────

	private ExecutionContext createInitialContext(InvokedCommand invoked, ClientSession session,
			CommandSource source) {
		return new ExecutionContext(invoked, session, source, null, Map.of(), null, -1);
	}

	private Optional<CommandSource> resolveSource(SenderContext sender) {
		return switch (sender) {
			case SenderContext.Player p -> {
				try {
					UUID u = UUID.fromString(p.uuid());
					yield proxy.getPlayer(u).map(pl -> (CommandSource) pl);
				} catch (Exception e) {
					yield Optional.empty();
				}
			}
			case SenderContext.Console() -> Optional.of(proxy.getConsoleCommandSource());
			default -> Optional.of(proxy.getConsoleCommandSource());
		};
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
				.map(raw -> {
					if (raw == null)
						return null;
					return raw.toString();
				})
				.orElse(null);
		return new InvokedCommand.TypedArgument(mapping.type(), value);
	}
}
