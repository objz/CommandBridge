package dev.objz.commandbridge.velocity.cmd;

import dev.jorel.commandapi.arguments.*;
import dev.objz.commandbridge.cmd.ArgumentMapper;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.velocity.cmd.custom.TimeArgument;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Maps ArgType enum values to CommandAPI Argument instances for Velocity
 */
public final class VelocityArgumentMapper implements ArgumentMapper {
	private final ProxyServer proxy;
	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	public VelocityArgumentMapper(ProxyServer proxy) {
		this.proxy = proxy;
	}

	@Override
	public Argument<?> map(ArgMapping argMapping) {
		if (argMapping == null || argMapping.name() == null || argMapping.name().isBlank()) {
			throw new IllegalArgumentException("Argument name cannot be null or blank");
		}
		if (argMapping.type() == null) {
			throw new IllegalArgumentException("ArgType cannot be null");
		}

		String argName = argMapping.name();
		ArgType type = argMapping.type();

		Argument<?> argument = switch (type) {
			case STRING -> new StringArgument(argName);
			case INTEGER -> new IntegerArgument(argName);
			case BOOLEAN -> new BooleanArgument(argName);
			case DOUBLE -> new DoubleArgument(argName);
			case TEXT -> new TextArgument(argName);
			case RANGE -> createRangeArgument(argName);

			case PLAYERS -> createPlayersArgument(argName, argMapping);
			case SERVER -> createServerArgument(argName, argMapping);
			case UUID -> createUuidArgument(argName);
			case TIME -> new TimeArgument(argName);

			default -> throw new UnsupportedOperationException(
					"ArgType." + type + " is not supported on Velocity. " +
							"Supported types: STRING, INTEGER, BOOLEAN, DOUBLE, TEXT, RANGE, PLAYERS, SERVER, UUID, TIME");
		};

		if (argMapping.suggestions() != null && !argMapping.suggestions().isEmpty()) {
			argument.includeSuggestions(ArgumentSuggestions.strings(
					argMapping.suggestions().toArray(String[]::new)));
		}

		return argument;
	}

	private Argument<?> createRangeArgument(String argName) {
		return new StringArgument(argName);
	}

	private Argument<?> createPlayersArgument(String argName, ArgMapping mapping) {
		StringArgument arg = new StringArgument(argName);

		arg.replaceSuggestions(ArgumentSuggestions.strings(info -> {
			List<String> playerNames = proxy.getAllPlayers().stream()
					.map(player -> player.getUsername())
					.collect(Collectors.toList());

			if (mapping.suggestions() != null && !mapping.suggestions().isEmpty()) {
				playerNames.addAll(mapping.suggestions());
			}

			return playerNames.toArray(String[]::new);
		}));

		return arg;
	}

	private Argument<?> createServerArgument(String argName, ArgMapping mapping) {
		StringArgument arg = new StringArgument(argName);

		arg.replaceSuggestions(ArgumentSuggestions.strings(info -> {
			List<String> serverNames = proxy.getAllServers().stream()
					.map(server -> server.getServerInfo().getName())
					.collect(Collectors.toList());

			if (mapping.suggestions() != null && !mapping.suggestions().isEmpty()) {
				serverNames.addAll(mapping.suggestions());
			}

			return serverNames.toArray(String[]::new);
		}));

		return arg;
	}

	private Argument<?> createUuidArgument(String argName) {
		StringArgument arg = new StringArgument(argName);

		arg.replaceSuggestions(ArgumentSuggestions.strings(info -> {
			return proxy.getAllPlayers().stream()
					.map(player -> player.getUniqueId().toString())
					.toArray(String[]::new);
		}));

		return arg;
	}

}
