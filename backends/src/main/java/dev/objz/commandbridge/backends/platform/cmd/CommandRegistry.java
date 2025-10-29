package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.backends.net.out.InvokedCommandEvent;
import dev.objz.commandbridge.cmd.ArgumentMapperInterface;
import dev.objz.commandbridge.cmd.CommandRegistryInterface;
import dev.objz.commandbridge.cmd.ref.EntityRef;
import dev.objz.commandbridge.cmd.ref.Location2D;
import dev.objz.commandbridge.cmd.ref.Location3D;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutboundRouter;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CommandRegistry implements CommandRegistryInterface {

	private final List<String> registeredCommands = new CopyOnWriteArrayList<>();
	private final ArgumentMapperInterface<Argument<?>> argumentMapper;
	private final OutboundRouter outRouter;

	public CommandRegistry(ArgumentMapperInterface<Argument<?>> mapper, OutboundRouter outRouter) {
		this.argumentMapper = mapper;
		this.outRouter = outRouter;
	}

	@Override
	public void register(CommandStub stub) throws Exception {
		String cmdName = stub.name();

		CommandAPICommand cmd = new CommandAPICommand(cmdName);

		if (stub.description() != null && !stub.description().isBlank()) {
			cmd.withShortDescription(stub.description());
			cmd.withFullDescription(stub.description());
		}

		if (stub.aliases() != null && !stub.aliases().isEmpty()) {
			cmd.withAliases(stub.aliases().toArray(String[]::new));
		}

		if (stub.args() != null && !stub.args().isEmpty()) {
			List<Argument<?>> arguments = new ArrayList<>(stub.args().size());
			for (ArgMapping argMapping : stub.args()) {
				Argument<?> argument = argumentMapper.map(argMapping);
				if (!argMapping.required()) {
					argument.setOptional(true);
				}
				arguments.add(argument);
			}
			cmd.withArguments(arguments);
		}

		cmd.executes((sender, args) -> {
			SenderContext senderCtx;
			if (sender instanceof org.bukkit.entity.Player p) {
				senderCtx = new SenderContext.Player(p.getName(), p.getUniqueId().toString());
			} else if (sender instanceof org.bukkit.command.ConsoleCommandSender) {
				senderCtx = new SenderContext.Console();
			} else if (sender instanceof org.bukkit.command.BlockCommandSender b) {
				var l = b.getBlock().getLocation();
				senderCtx = new SenderContext.Block(new Location3D(
						l.getWorld().getName(), l.getX(), l.getY(), l.getZ()));
			} else {
				senderCtx = new SenderContext.Other(sender.getClass().getSimpleName());
			}

			var typedArgs = new ArrayList<InvokedCommand.TypedArgument>();
			if (stub.args() != null && !stub.args().isEmpty()) {
				for (ArgMapping m : stub.args()) {
					String name = m.name();
					ArgType type = m.type();
					Object raw = args.getOptional(name).orElse(null);

					Object value = switch (type) {
						case STRING, TEXT -> (raw != null ? raw.toString() : null);

						case INTEGER, TIME -> (raw != null ? ((Number) raw).intValue() : 0);

						case DOUBLE -> (raw != null ? ((Number) raw).doubleValue() : 0.0);

						case BOOLEAN -> (raw instanceof Boolean b ? b
								: raw != null && Boolean.TRUE.equals(raw));

						case LOCATION -> {
							org.bukkit.Location l = (org.bukkit.Location) raw;
							yield (l == null) ? null
									: new Location3D(
											l.getWorld().getName(),
											l.getX(), l.getY(), l.getZ());
						}

						case LOCATION_2D -> {
							org.bukkit.Location l = (org.bukkit.Location) raw;
							yield (l == null) ? null
									: new Location2D(
											l.getWorld().getName(),
											l.getX(), l.getZ());
						}

						case PLAYERS, ENTITIES -> {
							var list = ((raw instanceof java.util.Collection<?>)
									? (java.util.Collection<?>) raw
									: List.of())
									.stream()
									.filter(o -> o instanceof org.bukkit.entity.Entity)
									.map(o -> {
										var e = (org.bukkit.entity.Entity) o;
										return new EntityRef(e.getType().name(),
												e.getUniqueId().toString(),
												e.getName());
									})
									.toList();
							yield list; // List<EntityRef>
						}

						case ENTITY_TYPE -> (raw != null ? raw.toString() : null);

						case RANGE -> (raw != null ? raw.toString() : null);
						case WORLD, ANGLE, ROTATION, ITEM_STACK, ENCHANTMENT, POTION_EFFECT,
								SOUND, BIOME, SERVER ->
							(raw != null ? raw.toString() : null);
					};

					typedArgs.add(new InvokedCommand.TypedArgument(type, value));
				}
			}

			// send over 
			outRouter.send(
					MessageType.INVOKED_COMMAND,
					new InvokedCommandEvent.Args(cmdName, typedArgs, senderCtx));

			// logging
			if (stub.args() != null && !stub.args().isEmpty()) {
				StringBuilder argLog = new StringBuilder("Args: ");
				boolean first = true;
				for (ArgMapping argMapping : stub.args()) {
					if (!first)
						argLog.append(", ");
					first = false;
					Object v = args.getOptional(argMapping.name()).orElse(null);
					argLog.append(argMapping.name()).append("=")
							.append(v != null ? v.toString() : "<not provided>");
				}
				Log.debug(argLog.toString());
			}
		});

		cmd.register();
		registeredCommands.add(cmdName);

		int argCount = stub.args() != null ? stub.args().size() : 0;
		int aliasCount = stub.aliases() != null ? stub.aliases().size() : 0;
		Log.debug("Registered command '{}' with {} arg(s) and {} alias(es)", cmdName, argCount, aliasCount);
	}

	@Override
	public void unregisterAll() throws Exception {
		for (String cmdName : registeredCommands) {
			try {
				CommandAPI.unregister(cmdName);
			} catch (Exception e) {
				Log.warn("Failed to unregister command '{}': {}", cmdName, e.getMessage());
			}
		}
		registeredCommands.clear();
	}
}
