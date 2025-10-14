package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.anno.Platform;
import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public final class PlatformProcessor implements PostProcessor {

	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		if (!Script.class.equals(buf.recordClass())) {
			return;
		}

		int idxRegister = -1;
		var comps = buf.components();

		for (int i = 0; i < comps.length; i++) {
			if ("register".equals(comps[i].getName())) {
				idxRegister = i;
				break;
			}
		}

		if (idxRegister < 0) {
			return;
		}

		Object registerObj = buf.get(idxRegister);
		if (!(registerObj instanceof List<?> registerList) || registerList.isEmpty()) {
			return;
		}

		Script script = reconstructScript(buf, ctx);
		if (script == null) {
			return;
		}

		List<ArgMapping> usedArgs = script.usedArguments();
		if (usedArgs.isEmpty()) {
			return;
		}

		Set<Location> registrationLocations = extractRegistrationLocations(registerList);
		if (registrationLocations.isEmpty()) {
			return;
		}

		for (ArgMapping arg : usedArgs) {
			if (arg == null || arg.type() == null) {
				continue;
			}

			Set<Location> supportedPlatforms = getSupportedPlatforms(arg.type());

			if (supportedPlatforms.isEmpty()) {
				continue;
			}

			Set<Location> unsupportedLocations = new HashSet<>(registrationLocations);
			unsupportedLocations.removeAll(supportedPlatforms);

			if (!unsupportedLocations.isEmpty()) {
				String argName = arg.name() != null ? arg.name() : "<unnamed>";
				String unsupportedStr = unsupportedLocations.stream()
						.map(Enum::name)
						.collect(Collectors.joining(", "));
				String supportedStr = supportedPlatforms.stream()
						.map(Enum::name)
						.collect(Collectors.joining(", "));

				ctx.problems().error(
						"register",
						String.format(
								"Argument '%s' (type '%s') is only supported on [%s], "
										+
										"but script registers on [%s]",
								argName,
								arg.type().name(),
								supportedStr,
								unsupportedStr));
			}
		}
	}

	private Script reconstructScript(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		try {
			var comps = buf.components();
			Object[] values = buf.values();
			Class<?>[] argTypes = Arrays.stream(comps)
					.map(c -> c.getType())
					.toArray(Class[]::new);
			var ctor = Script.class.getDeclaredConstructor(argTypes);
			ctor.setAccessible(true);
			return (Script) ctor.newInstance(values);
		} catch (Exception e) {
			ctx.problems().error("script", "Could not validate platform compatibility: " + e.getMessage());
			return null;
		}
	}

	private Set<Location> extractRegistrationLocations(List<?> registerList) {
		Set<Location> locations = new HashSet<>();

		for (Object obj : registerList) {
			if (obj instanceof IdMapping mapping && mapping.location() != null) {
				locations.add(mapping.location());
			}
		}

		return locations;
	}

	private Set<Location> getSupportedPlatforms(ArgType type) {
		try {
			Field field = ArgType.class.getField(type.name());
			Platform annotation = field.getAnnotation(Platform.class);

			if (annotation == null) {
				return Set.of();
			}

			return Set.of(annotation.value());
		} catch (NoSuchFieldException e) {
			return Set.of();
		}
	}
}
