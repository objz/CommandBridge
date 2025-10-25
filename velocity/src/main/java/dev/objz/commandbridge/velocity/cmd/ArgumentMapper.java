package dev.objz.commandbridge.velocity.cmd;

import com.velocitypowered.api.proxy.ProxyServer;

import dev.jorel.commandapi.arguments.*;
import dev.objz.commandbridge.cmd.ArgumentMapperInterface;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

public final class ArgumentMapper implements ArgumentMapperInterface<Argument<?>> {
	private final ProxyServer proxy;

	public ArgumentMapper(ProxyServer proxy) {
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

			case TIME -> new TimeArgument(argName);

			case SERVER -> new StringArgument(argName).includeSuggestions(
					ArgumentSuggestions.strings(
							proxy.getAllServers().stream()
									.map(server -> server.getServerInfo().getName())
									.toArray(String[]::new)));

			default -> throw new UnsupportedOperationException(
					"ArgType." + type + " is not supported on backends. " +
							"Supported types: all except SERVER");
		};

		if (argMapping.suggestions() != null && !argMapping.suggestions().isEmpty()) {
			argument.includeSuggestions(ArgumentSuggestions.strings(
					argMapping.suggestions().toArray(String[]::new)));
		}

		return argument;
	}
}
