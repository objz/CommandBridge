package dev.objz.commandbridge.bukkit.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BiomeArgument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EnchantmentArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatRangeArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.SoundArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import dev.jorel.commandapi.arguments.WorldArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;

import dev.objz.commandbridge.backends.PlatformRegistry;
import dev.objz.commandbridge.main.proto.cmd.CommandArg;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;

public final class CommandManager extends PlatformRegistry {
	private final JavaPlugin plugin;

	public CommandManager(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	@Override
	protected String platformName() {
		return "Bukkit";
	}

	@Override
	protected void doRegister(CommandStub stub) throws Exception {
		CommandAPICommand cmd = new CommandAPICommand(stub.name());

		var aliases = stub.aliases();
		if (aliases != null && !aliases.isEmpty()) {
			cmd.withAliases(aliases.toArray(String[]::new));
		}

		// Build required vs optional arguments
		List<Argument<?>> req = new ArrayList<>();
		List<Argument<?>> opt = new ArrayList<>();
		if (stub.args() != null) {
			for (CommandArg a : stub.args().stream().sorted((x, y) -> Integer.compare(x.index(), y.index()))
					.toList()) {
				Argument<?> arg = toArgument(a);
				if (a.required())
					req.add(arg);
				else
					opt.add(arg);
			}
		}

		if (!req.isEmpty())
			cmd.withArguments(req.toArray(Argument[]::new));
		if (!opt.isEmpty())
			cmd.withOptionalArguments(opt.toArray(Argument[]::new));

		// --- Demo executor so the command is executable ---
		cmd.executes((sender, arguments) -> {
			try {
				var defs = (stub.args() == null) ? List.<CommandArg>of()
						: stub.args().stream()
								.sorted((x, y) -> Integer.compare(x.index(), y.index()))
								.toList();

				int idx = 0;
				StringBuilder sb = new StringBuilder();
				for (var def : defs) {
					Object val;
					try {
						// CommandAPI's CommandArguments is NOT an array
						val = arguments.get(idx++);
					} catch (IndexOutOfBoundsException ignored) {
						break;
					}
					sb.append(def.name()).append('=').append(String.valueOf(val)).append(' ');
				}
				plugin.getLogger().info("[commandbridge] DEMO EXEC " + stub.name()
						+ " by " + sender.getName()
						+ " args: " + (sb.length() == 0 ? "<none>" : sb.toString().trim()));
			} catch (Exception ex) {
				plugin.getLogger().severe("[commandbridge] Demo exec failed for " + stub.name() + ": "
						+ ex.getMessage());
			}
		});
		// --------------------------------------------------

		cmd.register();
	}

	private Argument<?> toArgument(CommandArg a) {
		String name = a.name();
		ArgType t = a.type();

		return switch (t) {
			// Text/string-ish
			case TEXT -> new GreedyStringArgument(name);
			case WORD -> new StringArgument(name);

			// Choice: restrict input and suggest values
			case CHOICE -> {
				StringArgument sa = new StringArgument(name);
				var choices = (a.choices() == null) ? List.<String>of() : a.choices();
				if (!choices.isEmpty()) {
					sa = (StringArgument) sa.replaceSuggestions(
							ArgumentSuggestions.strings(choices.toArray(String[]::new)));
				}
				yield sa;
			}

			// Numbers / range
			// case RANGE -> {
			// Double min = (a.min() == null) ? null : a.min().doubleValue();
			// Double max = (a.max() == null) ? null : a.max().doubleValue();
			// yield (min != null || max != null)
			// ? new DoubleArgument(name, min, max)
			// : new DoubleArgument(name);
			// }
			//
			case RANGE -> new FloatRangeArgument(name);

			case NUMBER -> new DoubleArgument(name);

			case BOOLEAN -> new BooleanArgument(name);

			// Players / entities

			case PLAYER -> new EntitySelectorArgument.ManyPlayers(name);

			case ENTITY -> new EntitySelectorArgument.ManyEntities(name);

			// World / position
			case WORLD -> new WorldArgument(name);
			case LOCATION -> new LocationArgument(name, LocationType.PRECISE_POSITION);

			// Identifiers
			case UUID -> new UUIDArgument(name)
					.replaceSuggestions(ArgumentSuggestions
							.strings(info -> Bukkit.getOnlinePlayers().stream()
									.map(p -> p.getUniqueId().toString()) // shown
														// to
														// user
									.toArray(String[]::new))); // Minecraft-specific
													// extras
			case ITEM_STACK -> new ItemStackArgument(name);
			case ENCHANTMENT -> new EnchantmentArgument(name);
			case SOUND -> new SoundArgument(name);
			case BIOME -> new BiomeArgument(name);
		};
	}

	@Override
	protected void doUnregisterAll() throws Exception {
		// TODO: unregister using CommandAPI if desired
	}
}
