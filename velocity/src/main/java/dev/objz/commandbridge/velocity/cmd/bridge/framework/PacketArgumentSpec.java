package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PacketArgumentSpec(
		String parserKey,
		Optional<List<Object>> properties,
		Optional<String> suggestionsTypeKey) {

	public PacketArgumentSpec {
		Objects.requireNonNull(parserKey, "parserKey");
		properties = properties == null ? Optional.empty() : properties.map(List::copyOf);
		suggestionsTypeKey = suggestionsTypeKey == null ? Optional.empty() : suggestionsTypeKey;
	}

	public static PacketArgumentSpec ofParser(String parserKey) {
		return new PacketArgumentSpec(parserKey, Optional.empty(), Optional.empty());
	}

	public static PacketArgumentSpec ofParser(String parserKey, List<Object> properties) {
		return new PacketArgumentSpec(parserKey, Optional.of(List.copyOf(properties)), Optional.empty());
	}

	public PacketArgumentSpec withSuggestions(String suggestionsTypeKey) {
		return new PacketArgumentSpec(parserKey, properties, Optional.ofNullable(suggestionsTypeKey));
	}
}
