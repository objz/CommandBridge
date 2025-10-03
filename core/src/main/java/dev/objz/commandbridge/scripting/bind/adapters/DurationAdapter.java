package dev.objz.commandbridge.scripting.bind.adapters;

import java.lang.reflect.Type;
import java.time.Duration;

import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

public final class DurationAdapter implements TypeAdapter<Duration> {

	@Override
	public boolean supports(Type targetType) {
		return targetType == Duration.class;
	}

	@Override
	public Duration fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
		if (!(node instanceof YamlNode.Scalar s))
			throw new IllegalArgumentException("Expected scalar for Duration");
		Object v = s.value();
		if (v == null)
			return null;
		if (v instanceof Number n)
			return Duration.ofMillis(n.longValue());
		String str = v.toString().trim();
		return parseHumanDuration(str);
	}

	@Override
	public YamlNode toYaml(Duration value, Type targetType, ConvertContext ctx) {
		if (value == null)
			return YamlNode.scalar(null);
		long ms = value.toMillis();
		if (ms % 1000 == 0)
			return YamlNode.scalar((ms / 1000) + "s");
		return YamlNode.scalar(ms + "ms");
	}

	private static Duration parseHumanDuration(String s) {
		if (s.isEmpty())
			throw new IllegalArgumentException("empty duration");
		String lower = s.toLowerCase();
		if (lower.endsWith("ms")) {
			return Duration.ofMillis(Long.parseLong(lower.substring(0, lower.length() - 2)));
		} else if (lower.endsWith("s")) {
			return Duration.ofSeconds(Long.parseLong(lower.substring(0, lower.length() - 1)));
		} else if (lower.endsWith("m")) {
			return Duration.ofMinutes(Long.parseLong(lower.substring(0, lower.length() - 1)));
		} else if (lower.endsWith("h")) {
			return Duration.ofHours(Long.parseLong(lower.substring(0, lower.length() - 1)));
		} else {
			// plain number = seconds
			return Duration.ofSeconds(Long.parseLong(lower));
		}
	}
}
