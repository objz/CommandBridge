package dev.objz.commandbridge.main.scripting.v3.compiler.io;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationSerializer implements TypeSerializer<Duration> {
	public static final DurationSerializer INSTANCE = new DurationSerializer();

	private static final Pattern PATTERN = Pattern.compile("^\\s*(\\d+)\\s*(ms|s|m|h|d)?\\s*$",
			Pattern.CASE_INSENSITIVE);

	private DurationSerializer() {
	}

	@Override
	public Duration deserialize(Type type, ConfigurationNode node) throws SerializationException {
		if (node == null || node.virtual())
			return null;
		Object raw = node.raw();
		if (raw == null)
			return null;

		if (raw instanceof Number n) {
			return Duration.ofMillis(n.longValue());
		}

		String in = String.valueOf(raw).trim();
		if (in.isEmpty())
			return null;

		if (in.regionMatches(true, 0, "PT", 0, 2)) {
			try {
				return Duration.parse(in);
			} catch (Exception ignored) {
			}
		}

		Matcher m = PATTERN.matcher(in);
		if (!m.matches()) {
			throw new SerializationException(node, type,
					"Invalid duration '" + in
							+ "' (use 150ms, 2s, 3m, 1h, 1d or ISO-8601 like PT2S)");
		}

		long amount = Long.parseLong(m.group(1));
		String unit = (m.group(2) == null) ? "ms" : m.group(2).toLowerCase(Locale.ROOT);
		return switch (unit) {
			case "ms" -> Duration.ofMillis(amount);
			case "s" -> Duration.ofSeconds(amount);
			case "m" -> Duration.ofMinutes(amount);
			case "h" -> Duration.ofHours(amount);
			case "d" -> Duration.ofDays(amount);
			default -> throw new SerializationException(node, type, "Unknown unit: " + unit);
		};
	}

	@Override
	public void serialize(Type type, Duration obj, ConfigurationNode node) throws SerializationException {
		node.set(obj == null ? null : obj.toString());
	}
}
