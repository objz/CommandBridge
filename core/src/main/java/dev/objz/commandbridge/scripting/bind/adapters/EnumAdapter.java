package dev.objz.commandbridge.scripting.bind.adapters;

import java.lang.reflect.Type;

import dev.objz.commandbridge.scripting.bind.ConvertContext;
import dev.objz.commandbridge.scripting.bind.TypeAdapter;
import dev.objz.commandbridge.scripting.yaml.YamlNode;

public final class EnumAdapter implements TypeAdapter<Enum<?>> {

	@Override
	public boolean supports(Type targetType) {
		return targetType instanceof Class<?> c && c.isEnum();
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public Enum<?> fromYaml(YamlNode node, Type targetType, ConvertContext ctx) {
		if (!(targetType instanceof Class<?> c) || !c.isEnum())
			throw new IllegalArgumentException("Target not enum");
		if (!(node instanceof YamlNode.Scalar s))
			throw new IllegalArgumentException("Expected scalar for enum");
		Object v = s.value();
		if (v == null)
			return null;
		String name = v.toString();
		Class<? extends Enum> ec = (Class<? extends Enum>) c;
		for (Object constant : ec.getEnumConstants()) {
			Enum e = (Enum) constant;
			if (e.name().equalsIgnoreCase(name))
				return e;
		}
		try {
			return Enum.valueOf(ec, name);
		} catch (Exception ex) {
			throw new IllegalArgumentException(
					"Unknown enum constant: " + name + " for " + c.getSimpleName());
		}
	}

	@Override
	public YamlNode toYaml(Enum<?> value, Type targetType, ConvertContext ctx) {
		return YamlNode.scalar(value == null ? null : value.name());
	}
}
