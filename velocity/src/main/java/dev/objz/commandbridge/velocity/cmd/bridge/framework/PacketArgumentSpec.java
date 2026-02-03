package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import com.github.retrooper.packetevents.protocol.chat.Parsers;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PacketArgumentSpec(
		Parsers.Parser parser,
		Optional<List<Object>> properties,
		Optional<ResourceLocation> suggestionsType) {

	public PacketArgumentSpec {
		Objects.requireNonNull(parser, "parser");
		properties = properties == null ? Optional.empty() : properties.map(List::copyOf);
		suggestionsType = suggestionsType == null ? Optional.empty() : suggestionsType;
	}

	public static PacketArgumentSpec ofParser(Parsers.Parser parser) {
		return new PacketArgumentSpec(parser, Optional.empty(), Optional.empty());
	}

	public static PacketArgumentSpec ofParser(Parsers.Parser parser, List<Object> properties) {
		return new PacketArgumentSpec(parser, Optional.of(List.copyOf(properties)), Optional.empty());
	}

	public PacketArgumentSpec withSuggestions(ResourceLocation suggestionsType) {
		return new PacketArgumentSpec(parser, properties, Optional.ofNullable(suggestionsType));
	}
}
