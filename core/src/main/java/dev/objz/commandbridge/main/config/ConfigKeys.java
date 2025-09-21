package dev.objz.commandbridge.main.config;

import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.stream.Collectors;

public final class ConfigKeys {
	private ConfigKeys() {
	}

	public static Set<String> topLevelKeysOf(Class<?> recordType) {
		if (!recordType.isRecord())
			throw new IllegalArgumentException(recordType.getName() + " is not a record");

		return Arrays.stream(recordType.getRecordComponents())
				.map(rc -> yamlKeyFor(recordType, rc))
				.collect(Collectors.toUnmodifiableSet());
	}

	private static String yamlKeyFor(Class<?> recordType, RecordComponent rc) {
		try {
			Method m = recordType.getMethod(rc.getName());
			Setting s = m.getAnnotation(Setting.class);
			if (s != null && !s.value().isBlank())
				return s.value();
		} catch (NoSuchMethodException ignored) {
			/* impossible for records */ }

		try {
			Field f = recordType.getDeclaredField(rc.getName());
			Setting s = f.getAnnotation(Setting.class);
			if (s != null && !s.value().isBlank())
				return s.value();
		} catch (NoSuchFieldException ignored) {
			/* may be synthetic */ }

		Setting s = rc.getAnnotation(Setting.class);
		if (s != null && !s.value().isBlank())
			return s.value();

		return toKebab(rc.getName());
	}

	private static String toKebab(String camel) {
		return camel.replaceAll("(?<!^)([A-Z])", "-$1").toLowerCase(Locale.ROOT);
	}
}
